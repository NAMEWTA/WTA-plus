package org.namewta.sso.config;

import lombok.Data;

import java.time.Duration;

/** 数据库驱动的中央认证运行快照；启停和新会话期限按请求读取。 */
@Data
public class SsoProperties {
    @lombok.Getter(lombok.AccessLevel.NONE)
    @lombok.Setter(lombok.AccessLevel.NONE)
    private java.util.function.Supplier<org.namewta.sso.api.SsoRuntimeSettings> runtime;

    /** 生产只绑定MySQL配置服务，结构字段固定到维护重启。 */
    public void bind(java.util.function.Supplier<org.namewta.sso.api.SsoRuntimeSettings> reader) {
        runtime = reader;
        var value = reader.get();
        webOrigin = value.webOrigin();
        webBasePath = value.webBasePath();
        cookieName = value.cookieName();
        cookieSecure = value.cookieSecure();
    }

    /** 每个HTTP请求冻结一份动态快照，避免同一认证过程中混用两个版本。 */
    private org.namewta.sso.api.SsoRuntimeSettings snapshot() {
        var attributes =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes == null) return runtime.get();
        String key = "sso.runtime.settings";
        var value =
                (org.namewta.sso.api.SsoRuntimeSettings)
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

    /** 返回当前中央认证开关。 */
    public boolean isEnabled() {
        return runtime == null ? enabled : snapshot().enabled();
    }

    /** 返回新授权码TTL。 */
    public Duration getCodeTtl() {
        return runtime == null ? codeTtl : Duration.ofSeconds(snapshot().codeTtlSeconds());
    }

    /** 返回新中央会话TTL。 */
    public Duration getSessionTtl() {
        return runtime == null ? sessionTtl : Duration.ofSeconds(snapshot().sessionTtlSeconds());
    }

    /** 是否组装 SSO 运行时。 */
    private boolean enabled = true;

    /** sso-web 独立 Origin。 */
    private String webOrigin = "http://127.0.0.1:4176";

    /** sso-web 的 Vite/router base；独立 Origin 不包含该路径。 */
    private String webBasePath = "/";

    /**
     * 仅接受根路径或发布清单中的单层前缀，防止配置改变授权地址的 Origin。
     *
     * @param webBasePath 带首尾斜杠的静态页面 base
     */
    public void setWebBasePath(String webBasePath) {
        if (webBasePath == null || !webBasePath.matches("/(?:[A-Za-z0-9][A-Za-z0-9-]*/)?")) {
            throw new IllegalArgumentException("SSO web base path must be / or /prefix/");
        }
        this.webBasePath = webBasePath;
    }

    /** SSO 域会话 Cookie 名。 */
    private String cookieName = "Sso-Token";

    /** 默认HTTPS Cookie；显式关闭仅允许已声明的local/dev环境。 */
    private boolean cookieSecure = true;

    /** 授权码 TTL。 */
    private Duration codeTtl = Duration.ofMinutes(5);

    /** SSO 会话 TTL。 */
    private Duration sessionTtl = Duration.ofHours(8);
}
