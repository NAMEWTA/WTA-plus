package org.namewta.web.domain.vo;

import lombok.Data;
import org.namewta.system.password.PasswordPolicyProjection;

/**
 * 客户端公开认证上下文，仅返回前端展示注册入口所需字段。
 */
@Data
public class AuthClientContextVo {

    /**
     * 客户端是否可用
     */
    private Boolean clientEnabled;

    /**
     * 是否开放公开注册
     */
    private Boolean registerEnabled;

    /**
     * 当前可用客户端的非敏感密码规则。
     */
    private PasswordPolicyProjection passwordPolicy;

    /**
     * 当前 Client 是否启用第一方 SSO。
     */
    private Boolean ssoEnabled;

    /**
     * sso-web 授权页 URL。
     */
    private String ssoAuthorizeUrl;

    /**
     * 登录模式 local / sso / both。
     */
    private String authMode;

}
