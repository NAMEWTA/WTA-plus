package org.namewta.oidc.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.PageResult;
import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.domain.bo.*;
import org.namewta.oidc.domain.vo.*;
import org.namewta.oidc.service.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** 应用管理事务及公开只读目录。 */
@Component
@RequiredArgsConstructor
public class OidcApplicationUseCase {
    private final OidcApplicationService apps;
    private final OidcAuthorizationWorkflow authorizations;

    private final OidcKeyService keys;
    private final OidcProperties properties;

    /** 按确定顺序分页查询，限制单页记录数量。 */
    public PageResult<OidcApplicationVo> page(String name, int page, int size) {
        return apps.page(name, page, size);
    }

    /** 返回应用非机密详情。 */
    public OidcApplicationVo detail(Long id) {
        return apps.vo(apps.require(id));
    }

    @DSTransactional
    /** 在创建事务内返回新应用和仅可展示一次的客户端密钥。 */
    public OidcApplicationSecretVo create(OidcApplicationBo bo) {
        return apps.create(bo);
    }

    @DSTransactional
    /** 校验输入和乐观版本后更新允许编辑的字段。 */
    public OidcApplicationVo update(OidcApplicationBo bo) {
        var old = apps.vo(apps.require(bo.getApplicationId()));
        var result = apps.update(bo);
        if (!old.redirectUris().equals(result.redirectUris())
                || !old.clientAuthenticationMethod().equals(result.clientAuthenticationMethod())
                || old.pkceRequired() != result.pkceRequired())
            authorizations.revokeApplication(bo.getApplicationId());
        return result;
    }

    @DSTransactional
    /** 更新应用启用状态，停用时撤销既有授权。 */
    public OidcApplicationVo status(Long id, OidcStatusBo bo) {
        var result = apps.status(id, bo.version(), bo.enabled());
        if (!bo.enabled()) authorizations.revokeApplication(id);
        return result;
    }

    @DSTransactional
    /** 生成新的应用密钥，旧密钥与既有授权立即失效。 */
    public OidcApplicationSecretVo rotate(Long id, OidcVersionBo bo) {
        var result = apps.rotate(id, bo.version());
        authorizations.revokeApplication(id);
        return result;
    }

    @DSTransactional
    /** 按乐观版本删除应用并终止其既有授权。 */
    public void delete(Long id, OidcVersionBo bo) {
        apps.delete(id, bo.version());
        authorizations.revokeApplication(id);
    }

    /** 返回可授权身份字段目录，不返回实际资料。 */
    public List<OidcFieldVo> fields() {
        return org.namewta.oidc.support.OidcFieldCatalog.fields();
    }

    /** 返回发行方公开接入地址及当前签发就绪状态。 */
    public OidcProviderVo provider() {
        String issuer = properties.getIssuer();
        return new OidcProviderVo(
                issuer,
                issuer == null ? null : issuer + "/.well-known/openid-configuration",
                keys.ready(),
                properties.isEnabled(),
                List.of("openid", "profile", "email", "phone", "wta_person", "wta_enterprise"));
    }
}
