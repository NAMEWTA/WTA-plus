package org.namewta.profile.enterprise.controller.advice;

import org.namewta.profile.enterprise.controller.self.EnterpriseTransferController;
import org.namewta.profile.enterprise.domain.exception.EnterpriseTransferException;
import org.namewta.common.core.domain.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** EnterpriseTransferExceptionHandler 控制器，提供本能力的 HTTP 接口。 */
@RestControllerAdvice(assignableTypes = EnterpriseTransferController.class)
public class EnterpriseTransferExceptionHandler {

    /** 处理业务异常并返回统一响应。 */
    @ExceptionHandler(EnterpriseTransferException.class)
    public R<Void> handle(EnterpriseTransferException exception) {
        return R.fail(exception.getMessage());
    }
}
