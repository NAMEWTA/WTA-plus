package org.namewta.system.oss.exception;

/**
 * OSS 生命周期业务异常。
 */
public class OssLifecycleException extends RuntimeException {

    private final OssLifecycleError error;

    public OssLifecycleException(OssLifecycleError error, String message) {
        super(message);
        this.error = error;
    }

    public OssLifecycleException(OssLifecycleError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public OssLifecycleError error() {
        return error;
    }
}
