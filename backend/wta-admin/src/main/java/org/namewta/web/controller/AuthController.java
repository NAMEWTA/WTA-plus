package org.namewta.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.domain.model.LoginBody;
import org.namewta.common.core.utils.MessageUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.core.utils.ValidatorUtils;
import org.namewta.common.encrypt.annotation.ApiEncrypt;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.social.config.properties.SocialLoginConfigProperties;
import org.namewta.common.social.config.properties.SocialProperties;
import org.namewta.common.social.utils.SocialUtils;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.system.api.model.RegisterBody;
import org.namewta.system.api.model.SocialLoginBody;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.service.ISysSocialService;
import org.namewta.sso.config.SsoProperties;
import org.namewta.web.domain.vo.AuthClientContextVo;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.IAuthStrategy;
import org.namewta.web.service.SysLoginService;
import org.namewta.web.service.SysRegisterService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器，提供登录、注册、社交绑定和退出能力。
 *
 * @author Lion Li
 */
@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SocialProperties socialProperties;
    private final SysLoginService loginService;
    private final SysRegisterService registerService;
    private final ISysSocialService socialUserService;
    private final ISysClientService clientService;
    private final NotificationApplicationService notificationService;
    private final PasswordPolicyService passwordPolicyService;
    private final SsoProperties ssoProperties;


    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @ApiEncrypt
    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody String body) {
        LoginBody loginBody = JsonUtils.parseObject(body, LoginBody.class);
        ValidatorUtils.validate(loginBody);
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        SysClientVo client = clientService.queryByClientId(clientId);
        // 查询不到 client 或 client 内不包含 grantType
        if (ObjectUtil.isNull(client) || !StringUtils.contains(client.getGrantType(), grantType)) {
            log.info("客户端id: {} 认证类型：{} 异常!.", clientId, grantType);
            return R.fail(MessageUtils.message("auth.grant.type.error"));
        } else if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            return R.fail(MessageUtils.message("auth.grant.type.blocked"));
        }
        // 登录
        LoginVo loginVo = IAuthStrategy.login(body, client, grantType);

        Long userId = LoginHelper.getUserId();
        try {
            notificationService.submit(new NotificationCommand("admin-web", "auth-login", "LOGIN_SUCCESS",
                String.valueOf(userId), "USER", List.of(String.valueOf(userId)), "login-welcome",
                java.util.Map.of("title", "登录提醒", "content", "欢迎登录 WTA-Plus 后台管理系统"),
                List.of(NotificationChannel.IN_APP), NotificationStrategy.ALL, NotificationMode.ASYNC, 20,
                null, null, "login-welcome:" + userId + ":" + System.currentTimeMillis() / 60_000,
                java.util.Map.of()));
        } catch (RuntimeException exception) {
            // Login notification is best effort and must not turn a successful login into an error.
            log.warn("Failed to enqueue login notification for user {}", userId, exception);
        }
        return R.ok(loginVo);
    }

    /**
     * 获取第三方绑定跳转地址。
     *
     * @param source 登录来源
     * @return 跳转地址
     */
    @GetMapping("/binding/{source}")
    public R<String> authBinding(@PathVariable("source") String source) {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
        if (ObjectUtil.isNull(obj)) {
            return R.fail(source + "平台账号暂不支持");
        }
        AuthRequest authRequest = SocialUtils.getAuthRequest(source, socialProperties);
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return R.data(authorizeUrl);
    }

    /**
     * 处理前端回调后的社交账号绑定。
     *
     * @param loginBody 请求体
     * @return 操作结果
     */
    @PostMapping("/social/callback")
    public R<Void> socialCallback(@RequestBody SocialLoginBody loginBody) {
        // 校验token
        StpUtil.checkLogin();
        // 获取第三方登录信息
        AuthResponse<AuthUser> response = SocialUtils.loginAuth(
            loginBody.getSource(), loginBody.getSocialCode(),
            loginBody.getSocialState(), socialProperties);
        AuthUser authUserData = response.getData();
        // 判断授权响应是否成功
        if (!response.ok()) {
            return R.fail(response.getMsg());
        }
        loginService.socialRegister(authUserData);
        return R.ok();
    }


    /**
     * 取消当前用户的社交账号授权。
     *
     * @param socialId socialId
     * @return 操作结果
     */
    @DeleteMapping(value = "/unlock/{socialId}")
    public R<Void> unlockSocial(@PathVariable Long socialId) {
        // 校验token
        StpUtil.checkLogin();
        Boolean rows = socialUserService.deleteWithValidById(socialId);
        return rows ? R.ok() : R.fail("取消授权失败");
    }


    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

    /**
     * 查询客户端公开认证上下文。
     * <p>
     * 同时接受查询参数 {@code clientId} 与请求头 {@code clientid}（OAuth 客户端标识）。
     *
     * @param clientId       客户端标识（查询参数）
     * @param clientIdHeader 客户端标识（请求头）
     * @return 客户端是否可用及是否开放注册
     */
    @GetMapping("/client/context")
    public R<AuthClientContextVo> clientContext(@RequestParam(value = "clientId", required = false) String clientId,
                                                @RequestHeader(value = "clientid", required = false) String clientIdHeader) {
        AuthClientContextVo vo = new AuthClientContextVo();
        vo.setClientEnabled(false);
        vo.setRegisterEnabled(false);
        vo.setSsoEnabled(false);
        vo.setAuthMode("local");
        String resolvedClientId = StringUtils.isNotBlank(clientId) ? clientId : clientIdHeader;
        if (StringUtils.isBlank(resolvedClientId)) {
            return R.ok(vo);
        }
        SysClientVo client = clientService.queryByClientId(resolvedClientId);
        if (ObjectUtil.isNull(client)) {
            return R.ok(vo);
        }
        boolean clientEnabled = SystemConstants.NORMAL.equals(client.getStatus());
        vo.setClientEnabled(clientEnabled);
        vo.setRegisterEnabled(clientEnabled && Boolean.TRUE.equals(client.getRegisterEnabled()));
        String authMode = StringUtils.blankToDefault(client.getSsoAuthMode(), "both");
        vo.setAuthMode(authMode);
        boolean ssoEnabled = clientEnabled && ssoProperties.isEnabled() && Boolean.TRUE.equals(client.getSsoEnabled());
        vo.setSsoEnabled(ssoEnabled);
        if (ssoEnabled && StringUtils.isNotBlank(ssoProperties.getWebOrigin())) {
            String origin = StringUtils.trim(ssoProperties.getWebOrigin());
            if (origin.endsWith("/")) {
                origin = origin.substring(0, origin.length() - 1);
            }
            vo.setSsoAuthorizeUrl(origin + "/authorize");
        }
        if (clientEnabled) {
            vo.setPasswordPolicy(passwordPolicyService.publicProjection());
        }
        return R.ok(vo);
    }

    /**
     * 用户注册。
     *
     * @param user 注册信息
     * @return 操作结果
     */
    @ApiEncrypt
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody RegisterBody user) {
        registerService.register(user);
        return R.ok();
    }

}
