package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.Instant;

/** 个人绑定事件查询读模型。 */
@Data
public class PersonBindingEventRow {

    private Long personBindingEventId;
    private Long personBindingId;
    private Long personProfileId;
    private Long userId;
    private String eventType;
    private Integer bindingVersion;
    private String sourceType;
    private Long sourceId;
    private String reason;
    private Instant occurredTime;
}
