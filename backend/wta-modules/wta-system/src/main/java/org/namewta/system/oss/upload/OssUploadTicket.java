package org.namewta.system.oss.upload;

/**
 * Redis 中冻结的上传会话，不包含可长期复用的签名 URL。
 */
public record OssUploadTicket(
    String token,
    String policyKey,
    OssUploadMode mode,
    OssUploadState state,
    String service,
    String bucket,
    String objectKey,
    String uploadId,
    String originalName,
    String fileSuffix,
    long fileSize,
    String contentType,
    String fingerprint,
    String fingerprintDigest,
    Long userId,
    Long clientPk,
    long partSize,
    int partCount,
    long createdAt,
    long expiresAt,
    Long ossId
) {

    public OssUploadTicket withState(OssUploadState newState) {
        return new OssUploadTicket(token, policyKey, mode, newState, service, bucket, objectKey, uploadId,
            originalName, fileSuffix, fileSize, contentType, fingerprint, fingerprintDigest, userId, clientPk,
            partSize, partCount, createdAt, expiresAt, ossId);
    }

    public OssUploadTicket completed(Long completedOssId) {
        return new OssUploadTicket(token, policyKey, mode, OssUploadState.COMPLETED, service, bucket, objectKey,
            uploadId, originalName, fileSuffix, fileSize, contentType, fingerprint, fingerprintDigest, userId,
            clientPk, partSize, partCount, createdAt, expiresAt, completedOssId);
    }
}
