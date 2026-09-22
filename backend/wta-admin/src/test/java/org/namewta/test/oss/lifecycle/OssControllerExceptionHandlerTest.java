package org.namewta.test.oss.lifecycle;

import jakarta.servlet.http.HttpServletRequest;
import org.namewta.common.core.domain.R;
import org.namewta.system.controller.advice.OssControllerExceptionHandler;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 文件管理控制器把待删除访问拒绝转换成页面文案。
 */
@Tag("dev")
class OssControllerExceptionHandlerTest {

    @Test
    void pendingDeleteReturnsAFixedMessageWithoutObjectIdentifiers() {
        OssControllerExceptionHandler handler = new OssControllerExceptionHandler();
        HttpServletRequest request = request();

        R<Void> response = handler.handle(new OssLifecycleException(
            OssLifecycleError.OBJECT_DELETE_PENDING, "OSS 对象正在删除: 23886831"), request);

        assertEquals(500, response.getCode());
        assertEquals(OssControllerExceptionHandler.DELETED_MESSAGE, response.getMsg());
        assertFalse(response.getMsg().contains("23886831"));
    }

    @Test
    void otherLifecycleFailuresStayOnTheOpaqueOperatorMessage() {
        OssControllerExceptionHandler handler = new OssControllerExceptionHandler();

        R<Void> response = handler.handle(new OssLifecycleException(
            OssLifecycleError.OBJECT_NOT_FOUND, "OSS 对象不存在: missing-object-token"), request());

        assertTrue(response.getMsg().startsWith("发生未知异常，请联系管理员 [错误编号: "));
        assertFalse(response.getMsg().contains("该文件已删除"));
        assertFalse(response.getMsg().contains("missing-object-token"));
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/resource/oss/1/download-url");
        return request;
    }
}
