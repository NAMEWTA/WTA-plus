package org.namewta.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.namewta.system.domain.SysUserTypeRel;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户登录域关系视图对象 sys_user_type_rel
 *
 * @author NAMEWTA
 */
@Data
@AutoMapper(target = SysUserTypeRel.class)
public class SysUserTypeRelVo implements Serializable {

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
     * 登录域编码
     */
    private String userTypeCode;

    /**
     * 登录域名称
     */
    private String userTypeName;

    /**
     * 授权来源
     */
    private String grantSource;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 登录域状态（0正常 1停用）
     */
    private String userTypeStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
