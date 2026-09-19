package org.namewta.profile.enterprise.controller.advice;

import org.namewta.profile.enterprise.controller.self.EnterpriseApplicationController;
import org.namewta.profile.enterprise.domain.exception.EnterpriseApplicationException;
import org.namewta.common.core.domain.R;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** EnterpriseApplicationExceptionHandler 控制器，提供本能力的 HTTP 接口。 */
// 领域错误先于通用 RuntimeException 兜底，保留页面可识别的业务错误码。
@Order(0)
@RestControllerAdvice(basePackageClasses = EnterpriseApplicationController.class)
public class EnterpriseApplicationExceptionHandler {

    /** 处理业务异常并返回统一响应。 */
    @ExceptionHandler(EnterpriseApplicationException.class)
    public R<Void> handle(EnterpriseApplicationException exception) {
        return R.fail(exception.getMessage());
    }
}
