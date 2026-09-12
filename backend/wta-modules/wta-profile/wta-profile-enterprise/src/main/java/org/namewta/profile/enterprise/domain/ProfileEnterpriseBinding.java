package org.namewta.profile.enterprise.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 企业负责人当前绑定实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_enterprise_binding")
public class ProfileEnterpriseBinding extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("enterprise_binding_id")
    private Long enterpriseBindingId;
    private Long enterpriseProfileId;
    private Long userId;
    private String status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long effectiveUserId;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long effectiveProfileId;
    private Integer bindingVersion;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime boundTime;
    private LocalDateTime unboundTime;

    @Version
    private Integer version;

    @TableLogic
    private String delFlag;
}
