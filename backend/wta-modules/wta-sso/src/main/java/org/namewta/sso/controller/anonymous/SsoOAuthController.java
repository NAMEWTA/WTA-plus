package org.namewta.sso.controller.anonymous;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.domain.bo.SsoTokenBo;
import org.namewta.sso.domain.vo.SsoAuthorizeVo;
import org.namewta.sso.domain.vo.SsoTokenVo;
import org.namewta.sso.domain.SsoOAuthCommands;
import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.sso.usecase.SsoOAuthUseCase;
import org.namewta.sso.usecase.SsoSessionUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 Authorization Code + PKCE 匿名入口。
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sso/oauth2")
@ConditionalOnProperty(prefix = "namewta.sso", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SsoOAuthController {

    private final SsoOAuthUseCase oauthUseCase;
    private final SsoSessionUseCase sessionUseCase;
    private final SsoProperties properties;

    /**
     * 授权码请求。无 SSO 会话时返回 loginRequired。
     *
     * @param responseType         response_type
     * @param clientId             目标业务 Client
     * @param redirectUri          回调
     * @param state                CSRF state
     * @param codeChallenge        PKCE 挑战
     * @param codeChallengeMethod  挑战方法
     * @param request              用于读取 SSO Cookie
     * @return 授权结果
     */
    @GetMapping("/authorize")
    public R<SsoAuthorizeVo> authorize(@RequestParam("response_type") String responseType,
                                       @RequestParam("client_id") String clientId,
                                       @RequestParam("redirect_uri") String redirectUri,
                                       @RequestParam(value = "state", required = false) String state,
                                       @RequestParam(value = "code_challenge", required = false) String codeChallenge,
                                       @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
                                       HttpServletRequest request) {
        SsoAuthenticatedUser user = sessionUseCase.current(readSessionId(request));
        SsoOAuthCommands.AuthorizeResult result = oauthUseCase.authorize(
            new SsoOAuthCommands.AuthorizeCommand(
                responseType, clientId, redirectUri, state, codeChallenge, codeChallengeMethod, user));
        SsoAuthorizeVo vo = new SsoAuthorizeVo();
        vo.setLoginRequired(result.loginRequired());
        vo.setRedirectUri(result.redirectUri());
        return R.ok(vo);
    }

    /**
     * 用授权码与 PKCE verifier 换取业务 Token。
     *
     * @param bo 换票参数
     * @return 业务 Token
     */
    @Log(title = "SSO换票", businessType = BusinessType.OTHER, excludeParamNames = {"code", "codeVerifier", "code_verifier"}, isSaveResponseData = false)
    @PostMapping("/token")
    public R<SsoTokenVo> token(@RequestBody SsoTokenBo bo) {
        SsoBusinessTokenPort.IssuedToken issued = oauthUseCase.exchange(new SsoOAuthCommands.TokenCommand(
            bo.getGrantType(), bo.getCode(), bo.getRedirectUri(), bo.getClientId(), bo.getCodeVerifier()));
        SsoTokenVo vo = new SsoTokenVo();
        vo.setAccessToken(issued.accessToken());
        vo.setExpireIn(issued.expireIn());
        vo.setClientId(issued.clientId());
        return R.ok(vo);
    }

    /**
     * 撤销提交的业务 Token，不等于跨 App SLO。
     *
     * @param bo 含 token
     * @return 结果
     */
    @Log(title = "SSO撤销令牌", businessType = BusinessType.OTHER, excludeParamNames = {"token"})
    @PostMapping("/revoke")
    public R<Void> revoke(@RequestBody SsoTokenBo bo) {
        oauthUseCase.revoke(bo.getToken());
        return R.ok();
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
