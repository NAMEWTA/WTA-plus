package org.namewta.third.domain.vo;

import java.time.LocalDateTime;

public record ThirdProviderVo(Long providerId, String providerCode, String providerName, String baseUrl,
                              String status, Integer timeoutConnectMs, Integer timeoutReadMs,
                              Integer rateLimit, Integer concurrencyLimit, String sharedHeadersJson,
                              String remark, Integer version, LocalDateTime createTime, LocalDateTime updateTime) { }
