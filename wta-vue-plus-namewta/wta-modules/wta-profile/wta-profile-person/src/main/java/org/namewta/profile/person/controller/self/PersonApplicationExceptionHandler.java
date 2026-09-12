package org.namewta.profile.person.controller.self;

import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.common.core.domain.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** PersonApplicationExceptionHandler 控制器，提供本能力的 HTTP 接口。 */
@RestControllerAdvice(basePackageClasses = PersonApplicationController.class)
public class PersonApplicationExceptionHandler {

    /** 处理业务异常并返回统一响应。 */
    @ExceptionHandler(PersonApplicationException.class)
    public R<Void> handle(PersonApplicationException exception) {
        return R.fail(exception.getMessage());
    }
}
