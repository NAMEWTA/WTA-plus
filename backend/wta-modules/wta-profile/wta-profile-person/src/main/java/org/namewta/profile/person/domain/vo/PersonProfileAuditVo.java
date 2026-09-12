package org.namewta.profile.person.domain.vo;

import java.time.Instant;

/** PersonProfileAuditVo 对外返回模型。 */
public record PersonProfileAuditVo(
    long auditId,
    String operationType,
    long operatorUserId,
    String capability,
    String reason,
    String beforeStatus,
    String afterStatus,
    String result,
    String failureCategory,
    Instant occurredTime
) {
}
