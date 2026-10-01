package org.namewta.common.social.oidc;

/** 标准 Discovery 中本客户端实际消费的端点。 */
public record OidcMetadata(
        String issuer,
        String authorizationEndpoint,
        String tokenEndpoint,
        String jwksUri,
        String userInfoEndpoint,
        String endSessionEndpoint) {}
