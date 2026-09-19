package org.namewta.sso.adapter.http;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * SSO 域会话 Cookie：HttpOnly，由后端 Set-Cookie，禁止前端冒充。
 */
public final class SsoSessionCookie {

    private SsoSessionCookie() {
    }

    /**
     * 构造会话 Cookie。
     *
     * @param name   Cookie 名，默认 Sso-Token
     * @param value  会话标识；注销时为空
     * @param maxAge 有效期
     * @param secure 是否限制HTTPS；false仅由已验证的local/dev配置提供
     * @return ResponseCookie
     */
    public static ResponseCookie create(String name, String value, Duration maxAge, boolean secure) {
        Duration age = maxAge == null ? Duration.ZERO : maxAge;
        return ResponseCookie.from(name, value == null ? "" : value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite("Lax")
            .maxAge(age)
            .build();
    }
}
