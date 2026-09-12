package org.namewta.third.domain.row;

public record ThirdEndpointRow(Long endpointId, Long providerId, String providerCode, String endpointCode,
                               String endpointName, String httpMethod, String relativePath, String requestMode,
                               String responseMode, String pathSchemaJson, String querySchemaJson,
                               String headerSchemaJson, String bodySchemaJson, String responseSchemaJson,
                               String overrideJson, String status, Boolean idempotent, Integer rateLimit,
                               Integer concurrencyLimit, Integer retryCount, String sensitiveFieldsJson,
                               String adapterCode, Integer version) { }
