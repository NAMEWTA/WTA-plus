package org.namewta.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 登录域定义对象 sys_user_type
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_type")
public class SysUserType extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录域ID
     */
    @TableId(value = "user_type_id")
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
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}
