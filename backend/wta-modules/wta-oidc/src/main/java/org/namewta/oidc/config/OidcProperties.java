package org.namewta.oidc.config;

import lombok.Data;

/** 数据库驱动的协议运行快照；结构字段启动冻结，旧显式Java构造器保留兼容。 */
@Data
public class OidcProperties {
    @lombok.Getter(lombok.AccessLevel.NONE)
    @lombok.Setter(lombok.AccessLevel.NONE)
    private java.util.function.Supplier<org.namewta.oidc.domain.OidcServiceSettings> runtime;

    /** 生产实例只从数据库服务读取配置；保留无参构造与setter供存量显式装配。 */
    public void bind(
            java.util.function.Supplier<org.namewta.oidc.domain.OidcServiceSettings> reader) {
        this.runtime = reader;
        var value = reader.get();
        issuer = value.issuer();
        ssoWebUrl = value.ssoWebUrl();
        allowHttp = value.allowHttp();
    }

    /** 每个HTTP请求冻结一份动态快照，避免同一认证过程中混用两个版本。 */
    private org.namewta.oidc.domain.OidcServiceSettings snapshot() {
        var attributes =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes == null) return runtime.get();
        String key = "oidc.runtime.settings";
        var value =
                (org.namewta.oidc.domain.OidcServiceSettings)
                        attributes.getAttribute(
                                key,
                                org.springframework.web.context.request.RequestAttributes
                                        .SCOPE_REQUEST);
        if (value == null) {
            value = runtime.get();
            attributes.setAttribute(
                    key,
                    value,
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        }
        return value;
    }

    /** 动态开关仅控制新认证和新签发。 */
    public boolean isEnabled() {
        return runtime == null ? enabled : snapshot().enabled();
    }

    /** 新授权码期限不会改变既有持久截止。 */
    public int getCodeTtlSeconds() {
        return runtime == null ? codeTtlSeconds : snapshot().codeTtlSeconds();
    }

    /** 返回新访问凭据期限。 */
    public int getAccessTtlSeconds() {
        return runtime == null ? accessTtlSeconds : snapshot().accessTtlSeconds();
    }

    /** 返回新浏览器交互期限。 */
    public int getInteractionTtlSeconds() {
        return runtime == null ? interactionTtlSeconds : snapshot().interactionTtlSeconds();
    }

    private boolean enabled = true;
    private String issuer;
    private String ssoWebUrl;
    private String sessionCookieName = "Sso-Token";
    private boolean sessionCookieSecure = true;
    private String jwkSetFile;
    private String activeKid;
    private String stateEncryptionKey;
    private boolean allowHttp;
    private int codeTtlSeconds = 300;
    private int accessTtlSeconds = 600;
    private int interactionTtlSeconds = 300;
}
