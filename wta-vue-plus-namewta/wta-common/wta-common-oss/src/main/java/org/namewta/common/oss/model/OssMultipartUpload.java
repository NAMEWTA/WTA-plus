package org.namewta.common.oss.model;

/**
 * 已创建的 Multipart Upload 会话。
 */
public record OssMultipartUpload(String bucket, String key, String uploadId) {
}
