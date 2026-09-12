package org.namewta.common.notify.exception;

/**
 * 通知请求或渠道选择错误。
 */
public class NotifyValidationException extends RuntimeException {

    private final String code;

    public NotifyValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
