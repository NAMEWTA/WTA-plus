package org.namewta.common.notify.exception;

/**
 * 相同业务 Key 被用于不同通知内容。
 */
public class NotifyIdempotencyConflictException extends RuntimeException {

    private final String originalRequestId;

    public NotifyIdempotencyConflictException(String originalRequestId) {
        super("通知幂等 Key 与已有请求摘要冲突");
        this.originalRequestId = originalRequestId;
    }

    public String originalRequestId() {
        return originalRequestId;
    }
}
