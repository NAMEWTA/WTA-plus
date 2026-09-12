package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.Instant;

/** 个人绑定关系查询读模型。 */
@Data
public class PersonBindingRow {

    private Long personBindingId;
    private Long personProfileId;
    private Long userId;
    private String status;
    private Integer bindingVersion;
    private String sourceType;
    private Long sourceId;
    private Instant boundTime;
}
