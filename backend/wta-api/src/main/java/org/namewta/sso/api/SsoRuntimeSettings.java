package org.namewta.sso.api;

/** 中央会话配置。结构参数在启动时冻结，启停和新凭据期限可热更新。 */
public record SsoRuntimeSettings(
        boolean enabled,
        String webOrigin,
        String webBasePath,
        String cookieName,
        boolean cookieSecure,
        int codeTtlSeconds,
        int sessionTtlSeconds) {
    /** 未初始化时保持普通本地登录可用，不开放中央认证。 */
    public static SsoRuntimeSettings defaults() {
        return new SsoRuntimeSettings(false, "", "/", "Sso-Token", true, 300, 28800);
    }
}
