package org.namewta.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.namewta.system.domain.SysUserTypeRel;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户登录域关系业务对象 sys_user_type_rel
 *
 * @author NAMEWTA
 */
@Data
@AutoMapper(target = SysUserTypeRel.class, reverseConvertGenerate = false)
public class SysUserTypeRelBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关系ID
     */
    private Long relId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录域ID
     */
    private Long userTypeId;

    /**
     * 登录域ID列表（覆盖更新）
     */
    private List<Long> userTypeIds;

    /**
     * 授权来源
     */
    private String grantSource;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

}
