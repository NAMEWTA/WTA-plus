package org.namewta.sso.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SSO 密码登录输入。
 */
@Data
public class SsoLoginBo {

    /**
     * 本仓用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 本仓密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
