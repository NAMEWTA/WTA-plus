package org.namewta.third.domain.vo;

import java.time.LocalDate;

public record ThirdStatisticVo(String providerCode, String endpointCode, LocalDate statDate,
                               Long attemptCount, Long successCount, Long failureCount,
                               Long timeoutCount, Long rejectedCount, Long quotaValue) {
}
