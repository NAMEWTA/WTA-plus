package org.namewta.system.domain.vo;

/** 当前节点新鲜读取的 OIDC 元数据；不能据此判定凭据、回调或完整登录已经验证。 */
public record OidcMetadataDiagnosticVo(
    String issuer, String discoveryUrl, String authorizationEndpoint, String tokenEndpoint, String jwksUri,
    String userInfoEndpoint, String endSessionEndpoint, java.util.List<String> authenticationMethods,
    java.util.List<String> scopes, java.util.List<String> pkceMethods, java.util.List<String> responseTypes,
    java.util.List<String> signingAlgorithms, boolean backchannelLogoutSupported,
    boolean backchannelLogoutSessionSupported, java.time.Instant checkedAt) {
}
