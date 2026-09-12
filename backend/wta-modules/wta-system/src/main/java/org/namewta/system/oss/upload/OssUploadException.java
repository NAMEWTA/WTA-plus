package org.namewta.system.oss.upload;

/**
 * 上传控制面异常。
 */
public class OssUploadException extends RuntimeException {

    private final OssUploadError error;

    public OssUploadException(OssUploadError error, String message) {
        super(message);
        this.error = error;
    }

    public OssUploadException(OssUploadError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public OssUploadError error() {
        return error;
    }
}
