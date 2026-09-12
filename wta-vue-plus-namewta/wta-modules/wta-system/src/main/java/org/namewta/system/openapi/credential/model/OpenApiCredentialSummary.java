package org.namewta.system.openapi.credential.model;

import java.time.LocalDateTime;

/**
 * Persisted credential fields safe for repeated transport.
 */
public record OpenApiCredentialSummary(
    Long credentialId,
    Long ownerUserId,
    String appKey,
    String appName,
    String status,
    LocalDateTime expiresAt,
    String remark,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {
}
