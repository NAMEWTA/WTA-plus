package org.namewta.system.oss.migration;

import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.domain.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 把迁移失败的稳定错误码交给调用方，避免被通用运行时异常吞成「未知异常」。
 */
@Slf4j
@RestControllerAdvice
public class OssMigrationExceptionAdvice {

    @ExceptionHandler(OssMigrationException.class)
    public R<String> handle(OssMigrationException exception) {
        log.warn("OSS 迁移失败，错误码={}", exception.error().name());
        return R.fail(exception.getMessage(), exception.error().name());
    }
}
