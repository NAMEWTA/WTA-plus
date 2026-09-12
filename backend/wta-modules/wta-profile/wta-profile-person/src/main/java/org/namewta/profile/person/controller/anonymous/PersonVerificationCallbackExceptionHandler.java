package org.namewta.profile.person.controller.anonymous;

import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.common.core.domain.R;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** PersonVerificationCallbackExceptionHandler 控制器，提供本能力的 HTTP 接口。 */
@RestControllerAdvice(assignableTypes = PersonVerificationAnonymousController.class)
public class PersonVerificationCallbackExceptionHandler {

    /** 处理业务异常并返回统一响应。 */
    @ExceptionHandler(PersonVerificationException.class)
    public ResponseEntity<R<FailureResponse>> handle(PersonVerificationException failure) {
        HttpStatus status = switch (failure.category()) {
            case INVALID_SIGNATURE, EXPIRED_CALLBACK -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
        FailureResponse data = new FailureResponse(failure.category().name());
        return ResponseEntity.status(status)
            .body(R.fail("Person verification callback rejected", data));
    }

    /** 个人认证回调失败响应模型。 */
    public record FailureResponse(String category) {
    }
}
