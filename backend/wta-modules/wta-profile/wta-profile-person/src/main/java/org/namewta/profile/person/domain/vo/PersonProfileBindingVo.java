package org.namewta.profile.person.domain.vo;

import java.time.Instant;

/** PersonProfileBindingVo 对外返回模型。 */
public record PersonProfileBindingVo(
    long bindingId,
    long userId,
    String status,
    int bindingVersion,
    String sourceType,
    Long sourceId,
    Instant boundTime,
    Instant unboundTime
) {
}
