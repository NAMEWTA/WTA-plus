package org.namewta.oidc.adapter.api;

import org.namewta.oidc.usecase.OidcAuthorizationUseCase;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenIntrospection;
import org.springframework.security.oauth2.server.authorization.authentication.*;

/** 已认证第三方不能探查或撤销其他应用的 Token。 */
public final class OidcOwnedTokenProvider implements AuthenticationProvider {
    private final AuthenticationProvider delegate;
    private final OidcAuthorizationUseCase authorizations;

    /** 组装当前协议能力所需的明确依赖。 */
    public OidcOwnedTokenProvider(
            AuthenticationProvider delegate, OidcAuthorizationUseCase authorizations) {
        this.delegate = delegate;
        this.authorizations = authorizations;
    }

    @Override
    /** 在标准认证提供方前后增加凭据所有权或一次消费约束。 */
    public Authentication authenticate(Authentication a) {
        String token =
                a instanceof OAuth2TokenIntrospectionAuthenticationToken t
                        ? t.getToken()
                        : ((OAuth2TokenRevocationAuthenticationToken) a).getToken();
        if (!(a.getPrincipal() instanceof OAuth2ClientAuthenticationToken client)
                || !client.isAuthenticated()
                || client.getRegisteredClient() == null) {
            throw new OAuth2AuthenticationException("invalid_client");
        }
        boolean owned = false;
        try {
            var value = authorizations.token(token);
            owned =
                    value != null
                            && value.getRegisteredClientId()
                                    .equals(client.getRegisteredClient().getId());
        } catch (OAuth2AuthenticationException ignored) {
        }
        if (!owned) {
            if (a instanceof OAuth2TokenIntrospectionAuthenticationToken)
                return new OAuth2TokenIntrospectionAuthenticationToken(
                        token, client, OAuth2TokenIntrospection.builder().active(false).build());
            return a;
        }
        return delegate.authenticate(a);
    }

    @Override
    /** 保留被包装协议提供方支持的认证类型。 */
    public boolean supports(Class<?> type) {
        return delegate.supports(type);
    }
}
