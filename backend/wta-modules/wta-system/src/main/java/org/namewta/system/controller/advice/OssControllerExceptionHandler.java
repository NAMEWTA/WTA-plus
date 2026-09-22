package org.namewta.system.controller.advice;

import cn.hutool.core.util.RandomUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.domain.R;
import org.namewta.common.json.utils.LogSanitizer;
import org.namewta.system.controller.system.SysOssController;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 把文件管理接口上的待删除拒绝转换成页面能直接展示的文案。
 *
 * <p>该异常否则会落入通用运行时处理，并带上只供运维检索的错误编号。
 * 范围限于文件管理控制器，其它模块遇到同一生命周期异常时仍走原有处理。</p>
 */
@Slf4j
@Order(0)
@RestControllerAdvice(assignableTypes = SysOssController.class)
public class OssControllerExceptionHandler {

    public static final String DELETED_MESSAGE = "该文件已删除";

    /**
     * 待删除对象的下载和预览授权返回固定文案，不暴露对象编号或存储位置。
     *
     * @param exception 生命周期失败
     * @param request   当前请求，仅用于其它错误码的运维日志
     * @return 给页面的失败响应
     */
    @ExceptionHandler(OssLifecycleException.class)
    public R<Void> handle(OssLifecycleException exception, HttpServletRequest request) {
        if (exception.error() == OssLifecycleError.OBJECT_DELETE_PENDING) {
            log.warn("OSS 访问被拒绝，错误码={}", exception.error());
            return R.fail(DELETED_MESSAGE);
        }
        String errorId = RandomUtil.randomNumbers(8);
        log.error("请求地址'{}',发生未知异常, 错误编号: {}，类型={}",
            request.getRequestURI(), errorId, LogSanitizer.failure(exception));
        return R.fail("发生未知异常，请联系管理员 [错误编号: " + errorId + "]");
    }
}
