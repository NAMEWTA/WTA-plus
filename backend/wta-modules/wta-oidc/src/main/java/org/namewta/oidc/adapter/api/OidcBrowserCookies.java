package org.namewta.oidc.adapter.api;

import jakarta.servlet.http.*;

import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.support.OidcSecrets;
import org.springframework.http.ResponseCookie;

/** 读取 SSO host-only Cookie，并绑定短期 OIDC 事务到当前浏览器。 */
public final class OidcBrowserCookies {
    /** 工具类不允许实例化，调用方使用静态协议操作。 */
    private OidcBrowserCookies() {}

    /** 从当前请求读取指定 host-only Cookie。 */
    public static String read(HttpServletRequest request, String name) {
        if (request.getCookies() != null)
            for (var c : request.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    /** 读取或创建当前浏览器的随机交互绑定 Cookie。 */
    public static String ensure(
            HttpServletRequest request, HttpServletResponse response, OidcProperties properties) {
        String value = read(request, "Oidc-Browser");
        if (value == null || !value.matches("[A-Za-z0-9_-]{43}")) {
            value = OidcSecrets.random();
            response.addHeader(
                    "Set-Cookie",
                    ResponseCookie.from("Oidc-Browser", value)
                            .httpOnly(true)
                            .secure(!properties.isAllowHttp())
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(86400)
                            .build()
                            .toString());
        }
        return value;
    }
}
