package org.namewta.profile.enterprise.domain.vo;

import java.time.Instant;

/** EnterpriseProfileBindingVo 对外返回模型。 */
public record EnterpriseProfileBindingVo(
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
