package org.namewta.test.oss.access;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.system.api.OssService;
import org.namewta.system.controller.system.SysOssController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class OssAccessUrlContractUnitTest {

    @Test
    void apiExposesStructuredResolverAndKeepsPrivateCompatibilityMethods() throws Exception {
        Method resolve = OssService.class.getMethod("resolveAccessUrl", Long.class);
        Method defaultPrivate = OssService.class.getMethod("presignDownload", Long.class);
        Method namedPrivate = OssService.class.getMethod("presignDownload", Long.class, String.class);

        assertThat(resolve.getReturnType()).isEqualTo(OssService.OssAccessUrl.class);
        assertThat(defaultPrivate.getReturnType()).isEqualTo(OssService.OssDownloadUrl.class);
        assertThat(namedPrivate.getReturnType()).isEqualTo(OssService.OssDownloadUrl.class);
        OssService.OssAccessUrl publicUrl = new OssService.OssAccessUrl(
            "PUBLIC", "https://cdn.example.test/a%20b.txt", null, "a b.txt");
        OssService.OssAccessUrl privateUrl = new OssService.OssAccessUrl(
            "PRIVATE", "https://storage.example.test/signed", Instant.EPOCH, "private.txt");
        assertThat(publicUrl.expiresAt()).isNull();
        assertThat(privateUrl.expiresAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void marksLegacyBulkUrlMethodsAsDeprecatedCompatibilityOnly() throws Exception {
        Method selectUrlByIds = OssService.class.getMethod("selectUrlByIds", String.class);
        Method selectByIds = OssService.class.getMethod("selectByIds", String.class);

        assertCompatibilityDeprecation(selectUrlByIds);
        assertCompatibilityDeprecation(selectByIds);
    }

    @Test
    void managementDownloadEndpointUsesResolverAndRemainsAuthorized() throws Exception {
        Method endpoint = SysOssController.class.getDeclaredMethod("downloadUrl", Long.class);

        assertThat(endpoint.getGenericReturnType().getTypeName())
            .contains("org.namewta.system.api.OssService$OssAccessUrl");
        assertThat(endpoint.getAnnotation(GetMapping.class).value()).containsExactly("/{ossId}/download-url");
        assertThat(endpoint.getAnnotation(SaCheckPermission.class).value()).containsExactly("system:oss:download");
        assertThat(Arrays.stream(SysOssController.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(GetMapping.class) != null)
            .map(method -> String.join(",", method.getAnnotation(GetMapping.class).value())))
            .noneMatch(path -> path.contains("anonymous") || path.contains("public/{ossId}"));
    }

    private void assertCompatibilityDeprecation(Method method) {
        Deprecated deprecated = method.getAnnotation(Deprecated.class);
        assertThat(deprecated).as(method.getName()).isNotNull();
        assertThat(deprecated.since()).isEqualTo("6.0.0");
        assertThat(deprecated.forRemoval()).isFalse();
    }
}
