package org.namewta.system.domain.vo;

/** 交付给身份提供方的接入参数；不含密钥、密文或已登录会话。 */
public record ExternalAuthConnectionInfoVo(
    Long registrationId, String providerName, String providerKey, String protocol, String issuer,
    String discoveryUrl, String businessClientId, String externalClientId, String authenticationMethod,
    String appPublicUrl, String redirectUri, String postLogoutRedirectUri, java.util.List<String> scopes, String backchannelLogoutUri,
    boolean enabled, boolean secretConfigured) {
}
