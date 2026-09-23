package org.namewta.system.domain.bo;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.namewta.common.core.validation.ValidFormat;
import org.namewta.common.core.validation.ValidationFormat;
import org.namewta.common.core.xss.Xss;
import org.namewta.common.sensitive.annotation.Sensitive;
import org.namewta.common.sensitive.core.SensitiveStrategy;

import java.io.Serial;
import java.io.Serializable;

/**
 * 个人信息业务处理
 */

@Data
@NoArgsConstructor
public class SysUserProfileBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称
     */
    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;

    /**
     * 用户邮箱
     */
    @Sensitive(strategy = SensitiveStrategy.EMAIL)
    @ValidFormat(type = ValidationFormat.EMAIL, message = "{validation.email.invalid}")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;

    /**
     * 手机号码
     */
    @ValidFormat(type = ValidationFormat.MAINLAND_MOBILE, message = "{validation.phone.mobile.invalid}")
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    private String phoneNumber;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private String gender;

    /**
     * 头像 OSS ID
     */
    private Long avatar;

}
