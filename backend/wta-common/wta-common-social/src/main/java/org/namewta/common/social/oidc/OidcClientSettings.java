package org.namewta.common.social.oidc;

import java.util.List;

/** 单次协议调用的配置快照；不包含业务用户、角色或内部 Provider 类型。 */
public record OidcClientSettings(
        String issuer,
        String clientId,
        String clientSecret,
        String redirectUri,
        List<String> scopes,
        String authenticationMethod) {
    public OidcClientSettings {
        scopes = List.copyOf(scopes);
    }

    @Override
    public String toString() {
        return "OidcClientSettings[credentials=REDACTED]";
    }
}
