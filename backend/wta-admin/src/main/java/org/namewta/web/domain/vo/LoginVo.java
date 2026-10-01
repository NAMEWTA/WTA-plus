package org.namewta.web.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 登录成功后的令牌信息返回对象。
 */
@Data
public class LoginVo {

    /** 无令牌的首次登录分支：COMPLETE_PROFILE 或 BIND_REQUIRED。 */
    private String nextAction = "LOGIN_COMPLETE";
    private String registrationTicket;
    private java.util.List<String> requiredFields;
    private String message;
    private String authSource = "LOCAL";
    private boolean globalLogoutAvailable;

    /**
     * 授权令牌
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 授权令牌 access_token 的有效期
     */
    @JsonProperty("expire_in")
    private Long expireIn;

    /**
     * 刷新令牌 refresh_token 的有效期
     */
    @JsonProperty("refresh_expire_in")
    private Long refreshExpireIn;

    /**
     * 应用id
     */
    @JsonProperty("client_id")
    private String clientId;

    /**
     * 令牌权限
     */
    private String scope;

    /**
     * 用户 openid
     */
    private String openid;

}
