package org.namewta.system.api.model;

import java.util.List;
import java.util.Map;

/** 仅供服务端一次认证操作使用的不可变配置快照；回调必须通过 isCurrent 复核版本。 */
public record ExternalAuthRegistration(long id, long version, long providerVersion,
    String providerKey, String protocol, String issuer, String name, String icon,
    String businessClientId, String externalClientId, String clientSecret, String redirectUri,
    String postLogoutRedirectUri, List<String> scopes, String firstLoginPolicy, Map<String, String> options) {
    /** 拷贝可变输入，避免认证期间配置被调用方改写。 */
    public ExternalAuthRegistration {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
        options = options == null ? Map.of() : Map.copyOf(options);
    }
    /** 日志只输出版本身份，不输出凭据、地址或扩展配置。 */
    @Override
    public String toString() {
        return "ExternalAuthRegistration[id=" + id + ", version=" + version
            + ", providerVersion=" + providerVersion + ", credentials=<redacted>]";
    }
}
