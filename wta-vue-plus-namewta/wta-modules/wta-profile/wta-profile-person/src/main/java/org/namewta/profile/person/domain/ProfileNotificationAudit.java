package org.namewta.profile.person.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;

/** ProfileNotificationAudit 持久化实体模型。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("profile_notification_audit")
public class ProfileNotificationAudit extends BaseEntity {

    @TableId("notification_audit_id")
    private Long notificationAuditId;
    private String notificationType;
    private String profileType;
    private Long profileId;
    private Long applicationId;
    private Long targetUserId;
    private String notifyRequestId;
    private String status;
    private String failureCategory;
    private LocalDateTime occurredTime;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
