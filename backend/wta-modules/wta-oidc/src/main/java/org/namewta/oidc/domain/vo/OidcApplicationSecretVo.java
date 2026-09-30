package org.namewta.oidc.domain.vo;

/** 明文密钥只在创建或轮换当次返回。 */
public record OidcApplicationSecretVo(OidcApplicationVo application, String clientSecret) {}
