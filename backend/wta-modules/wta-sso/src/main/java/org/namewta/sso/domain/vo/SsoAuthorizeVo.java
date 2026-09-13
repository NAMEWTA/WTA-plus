package org.namewta.sso.domain.vo;

import lombok.Data;

/**
 * 授权结果。
 */
@Data
public class SsoAuthorizeVo {

    /**
     * 是否需要先登录 SSO
     */
    private Boolean loginRequired;

    /**
     * 带 code/state 的回调地址
     */
    private String redirectUri;
}
