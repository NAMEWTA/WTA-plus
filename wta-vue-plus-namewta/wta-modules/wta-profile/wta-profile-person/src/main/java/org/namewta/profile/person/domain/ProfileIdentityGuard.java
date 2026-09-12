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

/** ProfileIdentityGuard 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_identity_guard")
public class ProfileIdentityGuard extends BaseEntity {

    @TableId("identity_guard_id")
    private Long identityGuardId;
    private String profileType;
    private String identityKey;
    private String ownerType;
    private Long ownerId;
    private String status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String activeGuardKey;
    private LocalDateTime releasedTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
