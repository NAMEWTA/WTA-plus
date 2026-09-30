package org.namewta.oidc.adapter.api;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.usecase.OidcProtocolUseCase;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 标准客户端 SPI 只读投影，禁止动态客户端注册。 */
@Component
@RequiredArgsConstructor
public class OidcRegisteredClientRepository implements RegisteredClientRepository {
    private final OidcProtocolUseCase protocol;
    private final OidcProperties properties;

    @Override
    /** 拒绝框架动态写入，应用只可由管理用例创建和调整。 */
    public void save(RegisteredClient client) {
        throw new UnsupportedOperationException("Managed applications only");
    }

    @Override
    /** 按协议客户端或授权标识查询当前记录。 */
    public RegisteredClient findById(String id) {
        return findByClientId(id);
    }

    @Override
    /** 将当前启用的应用投影为标准客户端配置。 */
    public RegisteredClient findByClientId(String id) {
        var app = protocol.app(id);
        if (app == null || !Boolean.TRUE.equals(app.getEnabled())) return null;
        var view = protocol.view(app);
        return RegisteredClient.withId(view.clientId())
                .clientId(view.clientId())
                .clientName(view.name())
                .clientSecret(app.getClientSecretHash())
                .clientAuthenticationMethod(
                        new ClientAuthenticationMethod(view.clientAuthenticationMethod()))
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUris(v -> v.addAll(view.redirectUris()))
                .postLogoutRedirectUris(v -> v.addAll(view.postLogoutRedirectUris()))
                .scopes(v -> v.addAll(view.allowedScopes()))
                .clientSettings(
                        ClientSettings.builder()
                                .requireProofKey(view.pkceRequired())
                                .requireAuthorizationConsent(false)
                                .build())
                .tokenSettings(
                        TokenSettings.builder()
                                .authorizationCodeTimeToLive(
                                        Duration.ofSeconds(properties.getCodeTtlSeconds()))
                                .accessTokenTimeToLive(
                                        Duration.ofSeconds(properties.getAccessTtlSeconds()))
                                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                                .build())
                .build();
    }
}
