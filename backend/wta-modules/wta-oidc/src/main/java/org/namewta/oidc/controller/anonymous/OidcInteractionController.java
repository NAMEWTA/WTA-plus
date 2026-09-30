package org.namewta.oidc.controller.anonymous;

import cn.dev33.satoken.annotation.SaIgnore;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.R;
import org.namewta.oidc.adapter.api.OidcBrowserCookies;
import org.namewta.oidc.domain.vo.OidcLoginContextVo;
import org.namewta.oidc.usecase.*;
import org.springframework.web.bind.annotation.*;

/** SSO 登录页只获取合法 OIDC 事务的展示信息。 */
@SaIgnore
@RestController
@RequiredArgsConstructor
public class OidcInteractionController {
    private final OidcInteractionUseCase interactions;
    private final OidcProtocolUseCase protocol;

    @GetMapping("/oidc/login-context")
    /** 向 SSO 登录页提供已绑定事务的最小展示信息。 */
    public R<OidcLoginContextVo> context(
            @RequestParam("request") String tx, HttpServletRequest request) {
        var i = interactions.require(tx, OidcBrowserCookies.read(request, "Oidc-Browser"));
        var app = protocol.app(i.parameters().get("client_id"));
        if (app == null || !Boolean.TRUE.equals(app.getEnabled())) return R.fail("登录请求已失效");
        return R.ok(new OidcLoginContextVo(app.getName(), i.forceLogin()));
    }
}
