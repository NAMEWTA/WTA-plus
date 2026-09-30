package org.namewta.oidc.usecase;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Component;

/** 框架授权适配入口；不得把消费和签发包进同一个回滚事务。 */
@Component
@RequiredArgsConstructor
public class OidcAuthorizationUseCase {
    private final OidcAuthorizationWorkflow service;
    private final OidcConsumeUseCase consumption;

    /** 读取标识对应的当前持久记录，不存在时返回空。 */
    public OAuth2Authorization find(String id) {
        return service.find(id);
    }

    /** 通过凭据摘要查询授权，实时校验应用和账户状态。 */
    public OAuth2Authorization token(String raw) {
        return service.token(raw);
    }

    /** 在框架验证后协调首次授权保存或一次性消费与签发。 */
    public void save(OAuth2Authorization value, String code) {
        if (code == null) {
            service.saveInitial(value);
            return;
        }
        if (!consumption.consume(code, value.getRegisteredClientId()))
            throw new OAuth2AuthenticationException("invalid_grant");
        service.finish(value);
    }

    /** 删除短期上下文或撤销指定授权，不影响其他授权。 */
    public void remove(OAuth2Authorization value) {
        service.revoke(value.getId());
    }

    /** 以实际签发的 ID Token 摘要定位历史授权，仅供退出校验。 */
    public OAuth2Authorization logoutHint(String raw) {
        return service.logoutHint(raw);
    }

    /** 持久撤销当前 SSO 会话关联的授权。 */
    public void revokeSession(String sid) {
        service.revokeSession(sid);
    }
}
