package org.namewta.common.openapi.spi;

import java.time.Instant;

/**
 * Sensitive-data-free invocation metadata for optional metering extensions.
 */
public record OpenApiCallEvent(
    Long credentialId,
    Long ownerUserId,
    String interfaceId,
    String method,
    String path,
    int status,
    long durationMillis,
    Instant occurredAt
) {
}
