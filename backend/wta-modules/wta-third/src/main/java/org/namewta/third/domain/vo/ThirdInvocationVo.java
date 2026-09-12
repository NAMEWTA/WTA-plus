package org.namewta.third.domain.vo;

import java.time.LocalDateTime;

public record ThirdInvocationVo(Long invocationId, String requestId, String providerCode, String endpointCode,
                                Integer attemptCount, String logicalStatus, String failureCategory,
                                Integer httpStatus, Long durationMs, String sanitizedRequestJson,
                                String sanitizedResponseJson, LocalDateTime createTime) {
}
