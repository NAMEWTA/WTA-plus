package org.namewta.profile.person.controller.admin;

import org.namewta.profile.person.domain.exception.ProfileMaterialException;
import org.namewta.common.core.domain.R;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** ProfileMaterialExceptionHandler 控制器，统一处理材料业务异常。 */
// 领域错误先于通用 RuntimeException 兜底，保留页面可识别的业务错误码。
@Order(0)
@RestControllerAdvice(basePackages = {
    "org.namewta.profile.person.controller.admin",
    "org.namewta.profile.person.controller.self",
    "org.namewta.profile.enterprise.controller.admin",
    "org.namewta.profile.enterprise.controller.self"
})
public class ProfileMaterialExceptionHandler {

    /** 处理业务异常并返回统一响应。 */
    @ExceptionHandler(ProfileMaterialException.class)
    public R<Void> handle(ProfileMaterialException exception) {
        return R.fail(exception.getMessage());
    }
}
