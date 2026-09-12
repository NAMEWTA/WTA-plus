package org.namewta.system.oss.upload;

/**
 * Ticket 过期后仍可用于 Provider 补偿的最小记录。
 */
public record OssUploadCleanupRecord(
    String token,
    OssUploadMode mode,
    String service,
    String objectKey,
    String uploadId,
    long expiresAt
) {
}
