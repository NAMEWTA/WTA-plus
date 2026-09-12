package org.namewta.test.oss.cutover;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.system.controller.system.SysOssController;
import org.namewta.system.service.ISysOssService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OSS 旧字节协议删除后的后端合同测试。
 */
@Tag("dev")
class OssProtocolCutoverUnitTest {

    @Test
    void controllerMustExposeOnlyAuthorizedDownloadUrlInsteadOfByteRoutes() throws Exception {
        Method downloadUrl = SysOssController.class.getDeclaredMethod("downloadUrl", Long.class);

        assertArrayEquals(new String[]{"/{ossId}/download-url"},
            downloadUrl.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"system:oss:download"},
            downloadUrl.getAnnotation(SaCheckPermission.class).value());
        assertFalse(Arrays.stream(SysOssController.class.getDeclaredMethods())
            .anyMatch(method -> method.getName().equals("upload") || method.getName().equals("download")));
        assertFalse(Arrays.stream(SysOssController.class.getDeclaredMethods())
            .flatMap(method -> Arrays.stream(method.getParameterTypes()))
            .anyMatch(MultipartFile.class::equals));
        assertFalse(Arrays.stream(SysOssController.class.getDeclaredMethods())
            .map(Method::getReturnType).anyMatch(ResponseEntity.class::equals));
    }

    @Test
    void serviceContractMustNotAcceptMultipartFileOrReturnByteResponse() {
        assertFalse(Arrays.stream(ISysOssService.class.getDeclaredMethods())
            .flatMap(method -> Arrays.stream(method.getParameterTypes()))
            .anyMatch(MultipartFile.class::equals));
        assertFalse(Arrays.stream(ISysOssService.class.getDeclaredMethods())
            .map(Method::getReturnType).anyMatch(ResponseEntity.class::equals));
    }
}
