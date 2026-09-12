package org.namewta.profile.person.domain.vo;

import java.time.Instant;

/** PersonProfileSourceVo 对外返回模型。 */
public record PersonProfileSourceVo(
    long sourceId,
    String sourceType,
    long operatorUserId,
    String reason,
    String fieldSnapshotJson,
    Instant occurredTime
) {
}
