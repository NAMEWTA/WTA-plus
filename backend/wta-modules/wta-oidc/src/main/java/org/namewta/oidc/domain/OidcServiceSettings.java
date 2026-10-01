package org.namewta.oidc.domain;

import org.namewta.sso.api.SsoRuntimeSettings;

/** 服务管理目标配置。凭据独立管理，不在该响应中返回。 */
public record OidcServiceSettings(
        boolean enabled,
        String issuer,
        String ssoWebUrl,
        boolean allowHttp,
        int codeTtlSeconds,
        int accessTtlSeconds,
        int interactionTtlSeconds,
        SsoRuntimeSettings sso) {
    /** 无配置时只保留管理入口，不签发任何协议凭据。 */
    public static OidcServiceSettings defaults() {
        return new OidcServiceSettings(
                false, "", "", false, 300, 600, 300, SsoRuntimeSettings.defaults());
    }
}
