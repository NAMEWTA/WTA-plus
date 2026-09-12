package org.namewta.profile.enterprise.domain.vo;

import java.time.Instant;

/** EnterpriseProfileAuditVo 对外返回模型。 */
public record EnterpriseProfileAuditVo(
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
