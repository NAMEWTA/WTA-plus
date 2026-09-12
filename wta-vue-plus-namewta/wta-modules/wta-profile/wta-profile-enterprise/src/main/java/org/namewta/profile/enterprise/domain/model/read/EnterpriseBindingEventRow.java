package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

import java.time.Instant;

/** 企业绑定事件查询读模型。 */
@Data
public class EnterpriseBindingEventRow {

    private Long enterpriseBindingEventId;
    private Long enterpriseBindingId;
    private Long enterpriseProfileId;
    private Long userId;
    private String eventType;
    private Integer bindingVersion;
    private String sourceType;
    private Long sourceId;
    private String reason;
    private Instant occurredTime;
}
