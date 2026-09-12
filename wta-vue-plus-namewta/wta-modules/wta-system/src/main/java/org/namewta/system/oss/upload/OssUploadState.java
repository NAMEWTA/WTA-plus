package org.namewta.system.oss.upload;

/**
 * Redis 上传会话状态。
 */
public enum OssUploadState {
    INITIALIZED,
    UPLOADING,
    COMPLETING,
    COMPLETED,
    ABORTED,
    EXPIRED
}
