package org.namewta.profile.enterprise.domain.vo;

import java.time.Instant;

/** EnterpriseProfileSourceVo 对外返回模型。 */
public record EnterpriseProfileSourceVo(
    long sourceId,
    String sourceType,
    long operatorUserId,
    String reason,
    String fieldSnapshotJson,
    Instant occurredTime
) {
}
