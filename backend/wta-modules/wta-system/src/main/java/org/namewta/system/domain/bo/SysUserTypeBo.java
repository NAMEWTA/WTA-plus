package org.namewta.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.namewta.common.core.constant.RegexConstants;
import org.namewta.common.core.validate.AddGroup;
import org.namewta.common.core.validate.EditGroup;
import org.namewta.system.domain.SysUserType;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录域业务对象 sys_user_type
 */
@Data
@AutoMapper(target = SysUserType.class, reverseConvertGenerate = false)
public class SysUserTypeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录域ID
     */
    @NotNull(message = "登录域ID不能为空", groups = {EditGroup.class})
    private Long userTypeId;

    /**
     * 登录域编码
     */
    @NotBlank(message = "登录域编码不能为空", groups = {AddGroup.class})
    @Size(min = 0, max = 32, message = "登录域编码长度不能超过{max}个字符")
    @Pattern(regexp = RegexConstants.DICTIONARY_TYPE, message = "登录域编码必须以字母开头，且只能为（小写字母，数字，下划线）")
    private String userTypeCode;

    /**
     * 登录域名称
     */
    @NotBlank(message = "登录域名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 0, max = 30, message = "登录域名称长度不能超过{max}个字符")
    private String userTypeName;

    /**
     * 显示顺序
     */
    @NotNull(message = "显示顺序不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer orderNum;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

}
