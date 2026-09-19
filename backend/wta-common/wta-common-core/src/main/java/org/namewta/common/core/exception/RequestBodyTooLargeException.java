package org.namewta.common.core.exception;

import java.io.IOException;

/** 正文字节预算已耗尽；入口必须映射为413，不能作为日志失败降级后继续业务。 */
public final class RequestBodyTooLargeException extends IOException {
    public static final String ERROR_CODE = "REQUEST_BODY_TOO_LARGE";

    public RequestBodyTooLargeException() {
        super(ERROR_CODE);
    }
}
