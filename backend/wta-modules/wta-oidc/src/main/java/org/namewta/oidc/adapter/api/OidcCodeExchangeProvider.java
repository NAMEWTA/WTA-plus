package org.namewta.oidc.adapter.api;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.web.context.request.*;

/** 框架完成 PKCE/redirect/client 验证后，save 才进入一次性消费。 */
public final class OidcCodeExchangeProvider implements AuthenticationProvider {
    private final AuthenticationProvider delegate;

    /** 组装当前协议能力所需的明确依赖。 */
    public OidcCodeExchangeProvider(AuthenticationProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    /** 在标准认证提供方前后增加凭据所有权或一次消费约束。 */
    public Authentication authenticate(Authentication value) {
        var request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();
        request.setAttribute(
                OidcAuthorizationStore.CONSUMED,
                ((OAuth2AuthorizationCodeAuthenticationToken) value).getCode());
        try {
            return delegate.authenticate(value);
        } finally {
            request.removeAttribute(OidcAuthorizationStore.CONSUMED);
        }
    }

    @Override
    /** 保留被包装协议提供方支持的认证类型。 */
    public boolean supports(Class<?> type) {
        return delegate.supports(type);
    }
}
