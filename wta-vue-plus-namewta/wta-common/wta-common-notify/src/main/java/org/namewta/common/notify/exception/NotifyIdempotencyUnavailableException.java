package org.namewta.common.notify.exception;

/**
 * Redis 幂等状态无法建立或确认。
 */
public class NotifyIdempotencyUnavailableException extends RuntimeException {

    private final String phase;

    public NotifyIdempotencyUnavailableException(String phase, String message, Throwable cause) {
        super(message, cause);
        this.phase = phase;
    }

    public String phase() {
        return phase;
    }
}
