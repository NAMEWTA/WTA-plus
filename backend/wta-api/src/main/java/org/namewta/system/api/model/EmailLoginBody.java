package org.namewta.system.api.model;

import jakarta.validation.constraints.NotBlank;
import org.namewta.common.core.validation.ValidFormat;
import org.namewta.common.core.validation.ValidationFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.core.domain.model.LoginBody;

/**
 * 邮箱验证码登录请求对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmailLoginBody extends LoginBody {

    /**
     * 邮箱
     */
    @NotBlank(message = "{user.email.not.blank}")
    @ValidFormat(type = ValidationFormat.EMAIL, message = "{user.email.not.valid}")
    private String email;

    /**
     * 邮箱code
     */
    @NotBlank(message = "{email.code.not.blank}")
    private String emailCode;

}
