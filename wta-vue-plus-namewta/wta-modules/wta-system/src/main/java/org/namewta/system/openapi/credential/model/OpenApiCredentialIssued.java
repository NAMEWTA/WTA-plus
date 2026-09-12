package org.namewta.system.openapi.credential.model;

import java.time.LocalDateTime;

/**
 * One-time create/reset result. AppSecret is never part of a summary model.
 */
public record OpenApiCredentialIssued(
    Long credentialId,
    Long ownerUserId,
    String appKey,
    String appSecret,
    String appName,
    String status,
    LocalDateTime expiresAt,
    String remark,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {
}
