package org.namewta.third.domain.row;

public record ThirdProviderRow(Long providerId, String providerCode, String providerName, String baseUrl,
                               String status, Integer timeoutConnectMs, Integer timeoutReadMs,
                               Integer rateLimit, Integer concurrencyLimit, String sharedHeadersJson,
                               String remark, Integer version) { }
