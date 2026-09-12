package org.namewta.profile.enterprise.controller.advice;

import org.namewta.profile.enterprise.controller.anonymous.EnterpriseVerificationAnonymousController;
import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.common.core.domain.R;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** EnterpriseVerificationCallbackExceptionHandler 控制器，提供本能力的 HTTP 接口。 */
@RestControllerAdvice(assignableTypes = EnterpriseVerificationAnonymousController.class)
public class EnterpriseVerificationCallbackExceptionHandler {

    /** 处理业务异常并返回统一响应。 */
    @ExceptionHandler(EnterpriseVerificationException.class)
    public ResponseEntity<R<FailureResponse>> handle(EnterpriseVerificationException failure) {
        HttpStatus status = switch (failure.category()) {
            case INVALID_SIGNATURE, EXPIRED_CALLBACK -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
        FailureResponse data = new FailureResponse(failure.category().name());
        return ResponseEntity.status(status)
            .body(R.fail("Enterprise verification callback rejected", data));
    }

    /** 企业认证回调失败响应模型。 */
    public record FailureResponse(String category) {
    }
}
