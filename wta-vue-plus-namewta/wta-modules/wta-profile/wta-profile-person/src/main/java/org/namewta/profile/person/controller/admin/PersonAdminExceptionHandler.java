package org.namewta.profile.person.controller.admin;

import org.namewta.profile.person.domain.exception.PersonAdminException;
import org.namewta.common.core.domain.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** PersonAdminExceptionHandler 控制器，提供本能力的 HTTP 接口。 */
@RestControllerAdvice(basePackageClasses = PersonAdminController.class)
public class PersonAdminExceptionHandler {

    /** 处理业务异常并返回统一响应。 */
    @ExceptionHandler(PersonAdminException.class)
    public R<Void> handle(PersonAdminException exception) {
        return R.fail(exception.getMessage());
    }
}
