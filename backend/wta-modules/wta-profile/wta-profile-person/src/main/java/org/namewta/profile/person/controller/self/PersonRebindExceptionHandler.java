package org.namewta.profile.person.controller.self;

import org.namewta.profile.person.domain.exception.PersonRebindException;
import org.namewta.common.core.domain.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** PersonRebindExceptionHandler 控制器，提供本能力的 HTTP 接口。 */
@RestControllerAdvice(basePackageClasses = PersonRebindController.class)
public class PersonRebindExceptionHandler {

    /** 处理业务异常并返回统一响应。 */
    @ExceptionHandler(PersonRebindException.class)
    public R<Void> handle(PersonRebindException exception) {
        return R.fail(exception.getMessage());
    }
}
