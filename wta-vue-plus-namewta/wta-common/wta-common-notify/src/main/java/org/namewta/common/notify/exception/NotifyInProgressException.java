package org.namewta.common.notify.exception;

/**
 * 同一业务幂等 Key 已有请求执行中。
 */
public class NotifyInProgressException extends RuntimeException {

    private final String originalRequestId;

    public NotifyInProgressException(String originalRequestId) {
        super("相同通知请求正在发送中");
        this.originalRequestId = originalRequestId;
    }

    public String originalRequestId() {
        return originalRequestId;
    }
}
