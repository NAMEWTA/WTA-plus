package org.namewta.sso.controller.anonymous;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.domain.bo.SsoLoginBo;
import org.namewta.sso.usecase.SsoSessionUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * SSO 域密码登录与 HttpOnly Cookie 会话。
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sso")
@ConditionalOnProperty(prefix = "namewta.sso", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SsoSessionController {

    private final SsoSessionUseCase sessionUseCase;
    private final SsoProperties properties;

    /**
     * 本仓账号密码登录，由后端 Set-Cookie。
     *
     * @param bo       用户名密码
     * @param response 用于写入 Cookie
     * @return 结果
     */
    @Log(title = "SSO登录", businessType = BusinessType.OTHER, excludeParamNames = {"password"}, isSaveResponseData = false)
    @PostMapping("/login")
    public R<Void> login(@Valid @RequestBody SsoLoginBo bo, HttpServletResponse response) {
        String sessionId = sessionUseCase.login(bo.getUsername(), bo.getPassword());
        writeCookie(response, sessionId, properties.getSessionTtl());
        return R.ok();
    }

    /**
     * 查询当前 SSO 会话。
     *
     * @param request 请求
     * @return 是否已登录
     */
    @GetMapping("/session")
    public R<SsoAuthenticatedUser> session(HttpServletRequest request) {
        SsoAuthenticatedUser user = sessionUseCase.current(readSessionId(request));
        if (user == null) {
            return R.fail("未登录");
        }
        SsoAuthenticatedUser safe = new SsoAuthenticatedUser(user.getUserId(), user.getUsername());
        return R.ok(safe);
    }

    /**
     * 注销 SSO 会话。
     *
     * @param request  请求
     * @param response 响应
     * @return 结果
     */
    @Log(title = "SSO注销", businessType = BusinessType.OTHER)
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionUseCase.logout(readSessionId(request));
        writeCookie(response, "", Duration.ZERO);
        return R.ok();
    }

    private void writeCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(properties.getCookieName(), value)
            .httpOnly(true)
            .path("/")
            .sameSite("Lax")
            .maxAge(maxAge)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String readSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.getCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
