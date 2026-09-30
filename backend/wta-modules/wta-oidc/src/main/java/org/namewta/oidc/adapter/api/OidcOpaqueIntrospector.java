package org.namewta.oidc.adapter.api;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.usecase.OidcAuthorizationUseCase;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.resource.introspection.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** 只接受当前有效的 OIDC 不透明 Access Token。 */
@Component
@RequiredArgsConstructor
public class OidcOpaqueIntrospector implements OpaqueTokenIntrospector {
    private final OidcAuthorizationUseCase authorizations;

    @Override
    /** 仅接受当前有效且类型匹配的不透明 Access Token。 */
    public OAuth2AuthenticatedPrincipal introspect(String raw) {
        try {
            var value = authorizations.token(raw);
            if (value == null
                    || value.getAccessToken() == null
                    || !value.getAccessToken().isActive()
                    || !raw.equals(value.getAccessToken().getToken().getTokenValue()))
                throw new BadOpaqueTokenException("inactive_token");
            return new DefaultOAuth2AuthenticatedPrincipal(
                    value.getPrincipalName(),
                    Map.of(
                            "sub",
                            value.getPrincipalName(),
                            "scope",
                            value.getAccessToken().getToken().getScopes()),
                    List.of());
        } catch (OAuth2AuthenticationException ex) {
            throw new BadOpaqueTokenException("inactive_token");
        }
    }
}
