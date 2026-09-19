package org.namewta.system.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.core.domain.model.LoginBody;
import org.namewta.common.core.validation.ValidFormat;
import org.namewta.common.core.validation.ValidationFormat;
import org.hibernate.validator.constraints.Length;

/**
 * 用户注册对象
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterBody extends LoginBody {

    /**
     * 注册请求默认授权类型，避免继承 LoginBody 的 grantType 必填约束阻塞公开注册。
     */
    public RegisterBody() {
        setGrantType("password");
    }

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}")
    @Length(min = 2, max = 30, message = "{user.username.length.valid}")
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 可选邮箱。
     */
    @ValidFormat(type = ValidationFormat.EMAIL, message = "{validation.email.invalid}")
    @Length(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    /**
     * 注册必填手机号码；采用现有大陆手机号格式。
     */
    @NotBlank(message = "手机号码不能为空")
    @ValidFormat(type = ValidationFormat.MAINLAND_MOBILE, message = "{validation.phone.mobile.invalid}")
    private String phoneNumber;

}
