package org.namewta.oidc.service;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.oidc.dao.OidcServiceConfigDao;
import org.namewta.oidc.domain.OidcServiceSettings;
import org.namewta.oidc.support.OidcUriPolicy;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** MySQL目标配置与启动结构快照；Redis只缓存数据库版本对应的JSON。 */
@Service
@RequiredArgsConstructor
public class OidcConfigurationService {
    private final OidcServiceConfigDao dao;
    private volatile OidcServiceSettings structure;

    /** 启动固定发行方，热更新仅影响新请求的开关与期限。 */
    public synchronized OidcServiceSettings current() {
        var value = saved();
        if (structure == null) structure = value;
        return new OidcServiceSettings(
                value.enabled() && !structuralChange(structure, value),
                structure.issuer(),
                structure.ssoWebUrl(),
                structure.allowHttp(),
                value.codeTtlSeconds(),
                value.accessTtlSeconds(),
                value.interactionTtlSeconds(),
                value.sso());
    }

    /** 返回持久化目标，缺失基座时仍可运行普通本地认证。 */
    public OidcServiceSettings saved() {
        var row = dao.find();
        if (row == null || row.getSettingsJson() == null) return OidcServiceSettings.defaults();
        String cacheKey = "oidc:configuration:" + row.getVersion();
        String json = RedisUtils.getCacheObject(cacheKey);
        if (!java.util.Objects.equals(json, row.getSettingsJson())) {
            json = row.getSettingsJson();
            RedisUtils.setCacheObject(cacheKey, json, Duration.ofMinutes(10));
        }
        return JsonUtils.parseObject(json, OidcServiceSettings.class);
    }

    /** 所有密钥轮换与聚合管理操作共用固定配置行锁。 */
    public void lock() {
        if (dao.lock() == null) throw new ServiceException("请先安装认证配置基座");
    }

    /** 返回管理更新所需的当前版本。 */
    public int version() {
        var row = dao.find();
        return row == null ? 0 : row.getVersion();
    }

    /** 比较结构字段，调用方据此展示维护重启提示。 */
    public boolean structuralChange(OidcServiceSettings a, OidcServiceSettings b) {
        return !a.issuer().equals(b.issuer())
                || !a.ssoWebUrl().equals(b.ssoWebUrl())
                || a.allowHttp() != b.allowHttp();
    }

    /** 锁行校验并保存完整目标；结构调整不改变运行快照。 */
    public void save(int version, OidcServiceSettings value) {
        if (value == null
                || value.issuer() == null
                || value.ssoWebUrl() == null
                || value.sso() == null
                || value.codeTtlSeconds() < 30
                || value.codeTtlSeconds() > 3600
                || value.accessTtlSeconds() < 60
                || value.accessTtlSeconds() > 86400
                || value.interactionTtlSeconds() < 30
                || value.interactionTtlSeconds() > 3600) throw new ServiceException("OIDC服务配置无效");
        if (value.enabled() || !value.issuer().isBlank()) {
            OidcUriPolicy.issuer(value.issuer(), value.allowHttp());
            OidcUriPolicy.validate(value.ssoWebUrl(), value.allowHttp());
            if (!value.issuer().equals(OidcUriPolicy.origin(value.ssoWebUrl(), value.allowHttp())))
                throw new ServiceException("SSO页面必须与发行方同源");
        }
        var row = dao.lock();
        if (row == null) throw new ServiceException("请先安装认证配置基座");
        if (row.getVersion() != version) throw new ServiceException("配置已变更，请刷新");
        var old = JsonUtils.parseObject(row.getSettingsJson(), OidcServiceSettings.class);
        if (structuralChange(old, value) && (old.enabled() || value.enabled()))
            throw new ServiceException("请先停用OIDC，再保存结构配置并维护重启");
        if (value.enabled() && structuralChange(current(), value))
            throw new ServiceException("结构配置待生效，请维护重启后启用");
        row.setSettingsJson(JsonUtils.toJsonString(value));
        if (dao.update(row) != 1) throw new ServiceException("配置已变更，请刷新");
    }
}
