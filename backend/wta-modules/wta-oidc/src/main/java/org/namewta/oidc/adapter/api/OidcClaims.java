package org.namewta.oidc.adapter.api;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.domain.OidcPrincipal;
import org.namewta.oidc.usecase.OidcProtocolUseCase;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/** ID Token 只携带最小身份；可选资料统一通过 UserInfo 发布。 */
@Component
@RequiredArgsConstructor
public class OidcClaims implements OAuth2TokenCustomizer<JwtEncodingContext> {
    private final OidcProtocolUseCase protocol;
    private final OidcProperties properties;

    @Override
    /** 签发最小 ID Token，认证时间与有效期受真实 SSO 会话约束。 */
    public void customize(JwtEncodingContext context) {
        var p = (OidcPrincipal) context.getPrincipal().getPrincipal();
        protocol.require(p);
        context.getClaims()
                .subject(p.name())
                .claim("sid", org.namewta.oidc.support.OidcSecrets.hash(p.sessionId()))
                .claim("auth_time", Date.from(Instant.ofEpochSecond(p.authTime())))
                .expiresAt(
                        Instant.ofEpochSecond(
                                Math.min(
                                        p.expiresAt(),
                                        Instant.now().getEpochSecond()
                                                + properties.getAccessTtlSeconds())));
    }
}
