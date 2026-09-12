package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.Instant;

/** 个人通知审计持久化读模型，用于查询通知投递结果和失败原因。 */
@Data
public class PersonNotificationAuditRow {

    private Long notificationAuditId;
    private String notificationType;
    private Long profileId;
    private Long applicationId;
    private Long targetUserId;
    private String notifyRequestId;
    private String status;
    private String failureCategory;
    private Instant occurredTime;
    private Integer version;
}
