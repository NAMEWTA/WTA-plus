package org.namewta.system.oss.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

/**
 * OSS 业务引用。引用仅用于反向定位和生命周期保护，不承担权限判断。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_ref")
public class SysOssRef extends BaseEntity {

    @TableId("oss_ref_id")
    private Long ossRefId;

    private Long ossId;

    /** 实际物理表名。 */
    private String refType;

    /** 该物理表的真实主键字符串。 */
    private String refId;

    private Integer version;

    @TableLogic
    private String delFlag;
}
