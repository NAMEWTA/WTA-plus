package org.namewta.oidc.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.oidc.domain.OidcServiceSettings;
import org.namewta.oidc.domain.bo.OidcKeyBo;
import org.namewta.oidc.domain.bo.OidcServiceBo;
import org.namewta.oidc.domain.vo.OidcKeyVo;
import org.namewta.oidc.domain.vo.OidcLogoutDeliveryVo;
import org.namewta.oidc.domain.vo.OidcServiceVo;
import org.namewta.oidc.service.OidcConfigurationService;
import org.namewta.oidc.service.OidcKeyMaterialService;
import org.namewta.oidc.service.OidcLogoutService;
import org.namewta.sso.api.SsoRuntimeConfiguration;
import org.namewta.sso.api.SsoRuntimeSettings;
import org.namewta.sso.api.SsoSessionLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** 管理聚合用例：SSO配置与会话仅通过公共API接入，所有写入落同一主库。 */
@Service
@RequiredArgsConstructor
public class OidcServiceUseCase {
    private final OidcConfigurationService settings;
    private final OidcKeyMaterialService keys;
    private final OidcLogoutService logout;
    private final SsoRuntimeConfiguration sso;
    private final SsoSessionLifecycle sessions;
    private final Environment environment;

    /** 返回实际配置、目标配置及密钥就绪状态。 */
    public OidcServiceVo detail() {
        var active = withSso(settings.current(), sso.current());
        var saved = withSso(settings.saved(), sso.saved());
        return new OidcServiceVo(
                !saved.issuer().isBlank(),
                settings.version(),
                structureChanged(active, saved),
                active,
                saved,
                keys.ready("SIGNING"),
                keys.ready("STATE"));
    }

    /** 校验聚合配置与维护状态，版本冲突整体回滚。 */
    @DSTransactional
    public OidcServiceVo save(OidcServiceBo bo) {
        settings.lock();
        var value = bo.settings();
        if (value == null || value.sso() == null) throw new ServiceException("请填写完整服务配置");
        if (value.allowHttp() || !value.sso().cookieSecure()) {
            var profiles = environment.getActiveProfiles();
            if (profiles.length == 0
                    || Arrays.stream(profiles).anyMatch(p -> !Set.of("local", "dev").contains(p)))
                throw new ServiceException("HTTP认证只允许显式local/dev环境");
        }
        if (structureChanged(withSso(settings.saved(), sso.saved()), value)
                && (sessions.hasActiveSessions() || logout.unfinished()))
            throw new ServiceException("请先执行维护退出并等待关联应用退出投递完成");
        if (value.enabled() && (!keys.ready("SIGNING") || !keys.ready("STATE")))
            throw new ServiceException("请先生成签名键和状态键");
        settings.save(bo.version(), value);
        sso.save(value.sso());
        return detail();
    }

    /** 显式维护动作只在停止新认证后执行，持久预约每个关联RP退出。 */
    public void prepareRestart() {
        if (settings.current().enabled() || sso.current().enabled())
            throw new ServiceException("请先关闭OIDC与中央认证开关");
        sessions.revokeAllSessions();
    }

    /** 只读密钥元数据，无私钥读取或删除接口。 */
    public List<OidcKeyVo> keys() {
        return keys.list();
    }

    /** 轮换在同一配置行锁下串行，旧版本继续用于解密和验签。 */
    @DSTransactional
    public void generate(OidcKeyBo bo) {
        settings.lock();
        keys.generate(bo.kind());
    }

    /** 导入材料只写不读，与生成采用同一轮换边界。 */
    @DSTransactional
    public void importKey(OidcKeyBo bo) {
        settings.lock();
        keys.importKey(bo.kind(), bo.material());
    }

    /** 管理页查看有界投递状态。 */
    public PageResult<OidcLogoutDeliveryVo> deliveries(int page, int size) {
        return logout.page(page, size);
    }

    /** 管理员明确重排失败退出，成功或进行中任务不会重排。 */
    @DSTransactional
    public void retry(Long id) {
        logout.retry(id);
    }

    /** 每个owner返回自己的配置，不信任OIDC JSON中保存的SSO副本。 */
    private OidcServiceSettings withSso(OidcServiceSettings value, SsoRuntimeSettings central) {
        return new OidcServiceSettings(
                value.enabled(),
                value.issuer(),
                value.ssoWebUrl(),
                value.allowHttp(),
                value.codeTtlSeconds(),
                value.accessTtlSeconds(),
                value.interactionTtlSeconds(),
                central);
    }

    /** 只有身份和浏览器边界属于重启生效参数。 */
    private boolean structureChanged(OidcServiceSettings a, OidcServiceSettings b) {
        return settings.structuralChange(a, b)
                || !a.sso().webOrigin().equals(b.sso().webOrigin())
                || !a.sso().webBasePath().equals(b.sso().webBasePath())
                || !a.sso().cookieName().equals(b.sso().cookieName())
                || a.sso().cookieSecure() != b.sso().cookieSecure();
    }
}
