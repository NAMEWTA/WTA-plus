package org.namewta.common.notify.exception;

/**
 * 通知附件快照创建或物化失败。
 */
public class NotifyAttachmentSnapshotException extends RuntimeException {

    private final String code;

    public NotifyAttachmentSnapshotException(String code, String message) {
        super(message);
        this.code = code;
    }

    public NotifyAttachmentSnapshotException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
