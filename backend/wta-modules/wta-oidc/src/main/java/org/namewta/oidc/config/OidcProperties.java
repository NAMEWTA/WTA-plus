package org.namewta.oidc.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 第三方身份签发的部署配置；私钥和状态加密密钥不提供默认值。 */
@Data
@ConfigurationProperties("namewta.oidc")
public class OidcProperties {
    private boolean enabled = true;
    private String issuer;
    private String ssoWebUrl;
    private String sessionCookieName = "Sso-Token";
    private String jwkSetFile;
    private String activeKid;
    private String stateEncryptionKey;
    private boolean allowHttp;
    private int codeTtlSeconds = 300;
    private int accessTtlSeconds = 600;
    private int interactionTtlSeconds = 300;
}
