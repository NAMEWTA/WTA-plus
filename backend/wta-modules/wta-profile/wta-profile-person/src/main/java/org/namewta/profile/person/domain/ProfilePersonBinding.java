package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/** ProfilePersonBinding 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_person_binding")
public class ProfilePersonBinding extends BaseEntity {

    @TableId("person_binding_id")
    private Long personBindingId;
    private Long personProfileId;
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
