package org.namewta.common.openapi.spi;

import java.time.Instant;

/**
 * Decrypted credential material available only inside the machine-authentication boundary.
 */
public record OpenApiCredential(
    Long credentialId,
    Long ownerUserId,
    String appKey,
    String appSecret,
    Instant expiresAt
) {
}
