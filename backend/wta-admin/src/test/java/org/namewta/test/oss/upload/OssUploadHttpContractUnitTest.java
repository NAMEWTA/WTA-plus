package org.namewta.test.oss.upload;

import org.namewta.common.core.domain.R;
import org.namewta.system.controller.system.SysOssUploadController;
import org.namewta.system.oss.upload.OssUploadContracts.CompleteRequest;
import org.namewta.system.oss.upload.OssUploadContracts.ErrorResponse;
import org.namewta.system.oss.upload.OssUploadContracts.SignedPart;
import org.namewta.system.oss.upload.OssUploadError;
import org.namewta.system.oss.upload.OssUploadException;
import org.namewta.system.oss.upload.OssUploadService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 固定 REST 路径和 JSON 形状合同测试。
 */
@Tag("dev")
class OssUploadHttpContractUnitTest {

    @Test
    void shouldExposeOnlyFixedControlPlaneRoutes() {
        RequestMapping root = SysOssUploadController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/resource/oss/uploads"}, root.value());
        assertEquals("", path("init", PostMapping.class));
        assertEquals("/{uploadToken}/parts/sign", path("signParts", PostMapping.class));
        assertEquals("/{uploadToken}/parts", path("parts", GetMapping.class));
        assertEquals("/{uploadToken}/complete", path("complete", PostMapping.class));
        assertEquals("/{uploadToken}", path("abort", PostMapping.class));
        assertNull(method("abort").getAnnotation(DeleteMapping.class));
        assertTrue(method("complete").getGenericReturnType().getTypeName().contains("R<java.lang.String>"));
    }

    @Test
    void completeMustReturnOssIdAsResponseData() {
        OssUploadService uploadService = mock(OssUploadService.class);
        when(uploadService.complete(eq("token-1"), any(CompleteRequest.class))).thenReturn("9001");
        SysOssUploadController controller = new SysOssUploadController(uploadService);

        R<String> response = controller.complete("token-1", new CompleteRequest(List.of()));

        assertTrue(R.isSuccess(response));
        assertEquals("9001", response.getData());
        assertEquals("操作成功", response.getMsg());
    }

    @Test
    void signedPartMustExposeTransportFieldsWithoutNestedRequest() {
        Set<String> fields = Arrays.stream(SignedPart.class.getRecordComponents())
            .map(component -> component.getName()).collect(Collectors.toSet());
        assertEquals(Set.of("partNumber", "method", "url", "requiredHeaders", "expiresAt"), fields);
        assertEquals(Map.of(), new SignedPart(1, "PUT", "https://oss", null, null).requiredHeaders());
    }

    @Test
    void uploadErrorsMustRemainMachineDistinguishable() {
        SysOssUploadController controller = new SysOssUploadController(null);

        R<ErrorResponse> response = controller.handleUploadException(
            new OssUploadException(OssUploadError.FINGERPRINT_MISMATCH, "文件指纹不匹配"));

        assertFalse(R.isSuccess(response));
        assertEquals("FINGERPRINT_MISMATCH", response.getData().error());
        assertEquals("文件指纹不匹配", response.getMsg());
    }

    private Method method(String name) {
        return Arrays.stream(SysOssUploadController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
    }

    private <A extends java.lang.annotation.Annotation> String path(String methodName, Class<A> type) {
        A annotation = method(methodName).getAnnotation(type);
        if (annotation instanceof PostMapping mapping) {
            return mapping.value().length == 0 ? "" : mapping.value()[0];
        }
        if (annotation instanceof GetMapping mapping) {
            return mapping.value()[0];
        }
        throw new IllegalStateException("unsupported mapping");
    }
}
