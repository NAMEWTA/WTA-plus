package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

import java.time.Instant;

/** 企业绑定关系查询读模型。 */
@Data
public class EnterpriseBindingRow {

    private Long enterpriseBindingId;
    private Long enterpriseProfileId;
    private Long userId;
    private String status;
    private Integer bindingVersion;
    private String sourceType;
    private Long sourceId;
    private Instant boundTime;
}
