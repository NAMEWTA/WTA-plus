package org.namewta.oidc.controller.anonymous;

import cn.dev33.satoken.annotation.SaIgnore;

import jakarta.servlet.http.*;

import lombok.RequiredArgsConstructor;

import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.oidc.adapter.api.OidcBrowserCookies;
import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.domain.OidcPrincipal;
import org.namewta.oidc.support.OidcSecrets;
import org.namewta.oidc.usecase.OidcAuthorizationUseCase;
import org.namewta.oidc.usecase.OidcInteractionUseCase;
import org.namewta.oidc.usecase.OidcProtocolUseCase;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/** RP 发起退出必须使用真实签发记录，并由绑定浏览器的 CSRF 确认完成。 */
@SaIgnore
@RestController
@RequiredArgsConstructor
public class OidcLogoutController {
    private final OidcAuthorizationUseCase authorizations;
    private final OidcProtocolUseCase protocol;
    private final OidcInteractionUseCase interactions;
    private final OidcProperties properties;

    @GetMapping("/oidc/logout")
    /** 校验 RP 退出提示和精确回跳地址后展示 CSRF 确认页。 */
    public void begin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String hint = request.getParameter("id_token_hint");
            if (hint == null) throw new OAuth2AuthenticationException("invalid_request");
            var value = authorizations.logoutHint(hint);
            Authentication a = value.getAttribute(Principal.class.getName());
            var p = (OidcPrincipal) a.getPrincipal();
            String sid = OidcBrowserCookies.read(request, properties.getSessionCookieName());
            if (!OidcSecrets.equal(sid, p.sessionId()))
                throw new OAuth2AuthenticationException("invalid_request");
            var app = protocol.app(value.getRegisteredClientId());
            if (app == null) throw new OAuth2AuthenticationException("invalid_request");
            String target = request.getParameter("post_logout_redirect_uri");
            if (target != null
                    && !protocol.values(app.getPostLogoutRedirectUrisJson()).contains(target))
                throw new OAuth2AuthenticationException("invalid_request");
            String client = request.getParameter("client_id");
            if (client != null && !value.getRegisteredClientId().equals(client))
                throw new OAuth2AuthenticationException("invalid_request");
            Map<String, String> params = new LinkedHashMap<>();
            params.put("kind", "logout");
            params.put("client_id", value.getRegisteredClientId());
            if (target != null) params.put("target", target);
            String state = request.getParameter("state");
            if (state != null) {
                if (state.length() > 4096)
                    throw new OAuth2AuthenticationException("invalid_request");
                params.put("state", state);
            }
            String browser = OidcBrowserCookies.ensure(request, response, properties);
            String tx = interactions.create(params, browser, sid, false);
            var i = interactions.require(tx, browser);
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Referrer-Policy", "same-origin");
            response.setHeader(
                    "Content-Security-Policy",
                    "default-src 'none'; style-src 'self'; form-action 'self'"
                            + (target == null
                                    ? ""
                                    : " "
                                            + org.namewta.oidc.support.OidcUriPolicy.origin(
                                                    target, properties.isAllowHttp()))
                            + "; frame-ancestors 'none'; base-uri 'none'");
            response.getWriter()
                    .write(
                            "<!doctype html><html lang=zh-CN><meta charset=utf-8><meta"
                                    + " name=viewport"
                                    + " content='width=device-width,initial-scale=1'><link"
                                    + " rel=stylesheet href='"
                                    + escape(
                                            properties.getSsoWebUrl().replaceAll("/+$", "")
                                                    + "/oidc-theme.css")
                                    + "'><title>退出统一登录</title><main"
                                    + " class='oidc-card'><h1>退出统一登录</h1><p>确认结束当前 WTA"
                                    + " 登录会话。</p><form method=post action='/oidc/logout'><input"
                                    + " type=hidden name=oidc_request value='"
                                    + tx
                                    + "'><input type=hidden name=csrf value='"
                                    + i.csrf()
                                    + "'><button type=submit>确认退出</button></form></main></html>");
        } catch (OAuth2AuthenticationException ex) {
            error(response);
        } catch (RuntimeException ex) {
            unavailable(response);
        }
    }

    @PostMapping("/oidc/logout")
    @Log(
            title = "OIDC统一退出",
            businessType = BusinessType.OTHER,
            isSaveRequestData = false,
            isSaveResponseData = false)
    /** 校验 CSRF 和浏览器会话后消费退出交互，撤销当前会话授权并清除 SSO Cookie。 */
    public void finish(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String browser = OidcBrowserCookies.read(request, "Oidc-Browser"),
                    tx = request.getParameter("oidc_request");
            var i = interactions.require(tx, browser);
            String sid = OidcBrowserCookies.read(request, properties.getSessionCookieName());
            if (!"logout".equals(i.parameters().get("kind"))
                    || !OidcSecrets.equal(i.csrf(), request.getParameter("csrf"))
                    || !OidcSecrets.equal(i.previousSessionHash(), OidcSecrets.hash(sid)))
                throw new OAuth2AuthenticationException("invalid_request");
            var app = protocol.app(i.parameters().get("client_id"));
            String target = i.parameters().get("target");
            if (app == null
                    || target != null
                            && !protocol.values(app.getPostLogoutRedirectUrisJson())
                                    .contains(target))
                throw new OAuth2AuthenticationException("invalid_request");
            interactions.consume(tx, browser);
            protocol.logout(sid);
            response.addHeader(
                    "Set-Cookie",
                    ResponseCookie.from(properties.getSessionCookieName(), "")
                            .httpOnly(true)
                            .secure(properties.isSessionCookieSecure())
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(0)
                            .build()
                            .toString());
            if (target != null) {
                if (i.parameters().containsKey("state"))
                    target +=
                            (target.contains("?") ? "&" : "?")
                                    + "state="
                                    + URLEncoder.encode(
                                            i.parameters().get("state"), StandardCharsets.UTF_8);
                response.setStatus(303);
                response.setHeader("Location", target);
            } else {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter()
                        .write(
                                "<!doctype html><html lang=zh-CN><meta charset=utf-8><meta"
                                        + " name=viewport"
                                        + " content='width=device-width,initial-scale=1'><link"
                                        + " rel=stylesheet href='"
                                        + escape(
                                                properties.getSsoWebUrl().replaceAll("/+$", "")
                                                        + "/oidc-theme.css")
                                        + "'><title>已退出</title><main"
                                        + " class='oidc-card'><h1>已退出</h1><p>已退出 WTA"
                                        + " 统一登录。</p></main></html>");
            }
        } catch (OAuth2AuthenticationException ex) {
            error(response);
        } catch (RuntimeException ex) {
            unavailable(response);
        }
    }

    /** 转义部署地址的 HTML 属性字符，禁止配置穿透到页面标记。 */
    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** 退出校验失败只返回标准错误，不回显提示令牌或回跳参数。 */
    private void error(HttpServletResponse r) throws IOException {
        r.setStatus(400);
        r.setContentType("application/json");
        r.getWriter().write("{\"error\":\"invalid_request\"}");
    }

    /** 基础设施失败保留协议 HTTP 失败状态，避免统一业务异常处理器改写为 HTTP 200。 */
    private void unavailable(HttpServletResponse response) throws IOException {
        response.setStatus(503);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"error\":\"temporarily_unavailable\"}");
    }
}
