package org.namewta.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.web.domain.bo.ExternalAuthorizeBo;
import org.namewta.web.domain.bo.ExternalRegisterBo;
import org.namewta.web.domain.vo.ExternalAuthorizeVo;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.social.ExternalAuthAccountService;
import org.namewta.web.service.social.ExternalAuthService;
import org.namewta.web.service.social.ExternalAuthSessionStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 外部身份登录入口。匿名授权与已登录自服务分开验证，后台退出使用标准协议响应。 */
@RestController
@RequestMapping("/auth/social")
@RequiredArgsConstructor
public class ExternalAuthController {
    private final ExternalAuthService authentication;
    private final ExternalAuthAccountService accounts;
    private final ExternalAuthSessionStore sessions;

    @SaIgnore
    @PostMapping("/authorize")
    @Log(
            title = "发起第三方认证",
            businessType = BusinessType.GRANT,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<ExternalAuthorizeVo> authorize(@Valid @RequestBody ExternalAuthorizeBo request) {
        return R.ok(authentication.authorize(request));
    }

    @SaIgnore
    @PostMapping("/register")
    @Log(
            title = "第三方身份注册",
            businessType = BusinessType.INSERT,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<LoginVo> register(@Valid @RequestBody ExternalRegisterBo request) {
        return R.ok(authentication.register(request));
    }

    @GetMapping("/bindings")
    public R<List<ExternalAuthAccountService.Binding>> bindings() {
        return R.ok(authentication.bindings());
    }

    public record Unbind(@NotNull Long id) {}

    @PostMapping("/unbind")
    @Log(
            title = "解绑第三方身份",
            businessType = BusinessType.DELETE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<Void> unbind(@Valid @RequestBody Unbind request) {
        StpUtil.checkLogin();
        accounts.unbind(LoginHelper.getUserId(), request.id());
        return R.ok();
    }

    @GetMapping("/session")
    public R<ExternalAuthSessionStore.Status> session() {
        return R.ok(sessions.status());
    }

    @PostMapping("/logout")
    @Log(
            title = "发起SSO统一退出",
            businessType = BusinessType.OTHER,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public R<ExternalAuthSessionStore.Logout> logout() {
        return R.ok(sessions.beginLogout());
    }

    @SaIgnore
    @PostMapping(
            value = "/backchannel/{registrationId}",
            consumes = "application/x-www-form-urlencoded")
    @Log(
            title = "接收OIDC退出通知",
            businessType = BusinessType.OTHER,
            isSaveRequestData = false,
            isSaveResponseData = false)
    public ResponseEntity<Void> backchannel(
            @PathVariable long registrationId,
            @RequestParam(value = "logout_token", required = false) String token) {
        if (token == null || token.isBlank())
            return ResponseEntity.badRequest().header("Cache-Control", "no-store").build();
        try {
            authentication.backchannel(registrationId, token);
            return ResponseEntity.ok().header("Cache-Control", "no-store").build();
        } catch (org.namewta.common.core.exception.ServiceException exception) {
            int status = Integer.valueOf(503).equals(exception.getCode()) ? 503 : 400;
            return ResponseEntity.status(status).header("Cache-Control", "no-store").build();
        } catch (RuntimeException exception) {
            // 让提供方重试基础设施故障；不能被业务异常处理包装为 HTTP 200 而误确认退出成功。
            return ResponseEntity.status(503).header("Cache-Control", "no-store").build();
        }
    }
}
