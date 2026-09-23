package org.namewta.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 用户登录域关系对象 sys_user_type_rel
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_type_rel")
public class SysUserTypeRel extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关系ID
     */
    @TableId(value = "rel_id")
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
     * 授权来源
     */
    private String grantSource;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

}
