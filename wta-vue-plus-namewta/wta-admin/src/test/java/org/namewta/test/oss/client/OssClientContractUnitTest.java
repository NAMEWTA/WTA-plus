package org.namewta.test.oss.client;

import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.model.OssClientCapabilities;
import org.namewta.common.oss.model.OssObjectOptions;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class OssClientContractUnitTest {

    @Test
    void structuredPresignCarriesBrowserExecutableContractAndCanonicalMultipartQuery() throws Exception {
        Instant before = Instant.now();
        try (OssClient client = new DefaultOssClientImpl("test", clientConfig())) {
            OssObjectOptions options = new OssObjectOptions(
                "video/mp4",
                Map.of("upload-ticket", "ticket-1"),
                null
            );

            OssPresignedRequest put = client.presignPut("videos/demo.mp4", Duration.ofMinutes(5), options);
            OssPresignedRequest part = client.presignUploadPart(
                "videos/demo.mp4",
                "upload-123",
                7,
                Duration.ofMinutes(5)
            );

            assertEquals("PUT", put.method());
            assertEquals("video/mp4", put.requiredHeaders().get("content-type"));
            assertEquals("ticket-1", put.requiredHeaders().get("x-amz-meta-upload-ticket"));
            assertTrue(put.expiresAt().isAfter(before.plus(Duration.ofMinutes(4))));
            assertTrue(part.url().contains("partNumber=7"));
            assertTrue(part.url().contains("uploadId=upload-123"));
            assertEquals("PUT", part.method());
        }
    }

    @Test
    void additiveDirectTransferMethodsKeepAwsModelsBehindTheClientInterface() {
        OssClientCapabilities capabilities = OssClientCapabilities.s3CompatibleBaseline();

        assertTrue(capabilities.multipartUpload());
        assertTrue(capabilities.copyObject());
        assertTrue(capabilities.checksumAlgorithms().isEmpty());

        for (Method method : OssClient.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase(Locale.ROOT);
            if (!methodName.matches("(capabilities|(bucket)?(headobject|presignput|presignget|createmultipartupload|presignuploadpart|listparts|completemultipartupload|abortmultipartupload|copyobject))")) {
                continue;
            }
            assertFalse(method.getReturnType().getName().startsWith("software.amazon.awssdk"), method::toString);
            assertTrue(Arrays.stream(method.getParameterTypes())
                .noneMatch(type -> type.getName().startsWith("software.amazon.awssdk")), method::toString);
        }
    }

    private OssClientConfig clientConfig() {
        return OssClientConfig.builder()
            .endpoint("localhost:9000")
            .domain("cdn.example.test/assets")
            .useHttps(true)
            .usePathStyleAccess(true)
            .accessKey("access-key")
            .secretKey("secret-key")
            .bucket("media")
            .prefix("uploads")
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT)
            .build();
    }
}
