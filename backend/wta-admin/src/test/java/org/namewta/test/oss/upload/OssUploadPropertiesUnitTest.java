package org.namewta.test.oss.upload;

import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.system.oss.upload.OssUploadException;
import org.namewta.system.oss.upload.OssUploadMode;
import org.namewta.system.oss.upload.OssUploadProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 上传策略启动校验测试。
 */
@Tag("dev")
class OssUploadPropertiesUnitTest {

    @Test
    void shouldAcceptBoundedPolicyAndResolveAutoMode() {
        OssUploadProperties properties = validProperties();
        assertDoesNotThrow(properties::validate);
        assertEquals(OssUploadMode.SINGLE, properties.requirePolicy("general").resolveMode(10));
        assertEquals(OssUploadMode.MULTIPART,
            properties.requirePolicy("general").resolveMode(100L * 1024 * 1024));
        assertEquals(Duration.ofMinutes(5), properties.getPresignTtl());
        assertEquals(AccessPolicy.PRIVATE, properties.requirePolicy("general").getExpectedAccessPolicy());
    }

    @Test
    void singleModeDoesNotNeedMultipartParameters() {
        OssUploadProperties.Policy policy = new OssUploadProperties.Policy();
        policy.setMode(OssUploadMode.SINGLE);
        policy.setPartSize(0);
        policy.setMultipartThreshold(0);
        policy.setMaxSize(10L * 1024 * 1024);
        policy.setAllowedContentTypes(Set.of("image/png"));
        policy.setObjectPrefix("direct/avatar");
        policy.setExpectedAccessPolicy(AccessPolicy.PRIVATE);
        OssUploadProperties properties = new OssUploadProperties();
        properties.setPolicies(Map.of("avatar", policy));
        assertDoesNotThrow(properties::validate);
        assertEquals(OssUploadMode.SINGLE, properties.requirePolicy("avatar").resolveMode(500L * 1024 * 1024));
    }

    @Test
    void shouldRejectMissingAccessPolicy() {
        OssUploadProperties properties = validProperties();
        properties.getPolicies().get("general").setExpectedAccessPolicy(null);
        assertThrows(OssUploadException.class, properties::validate);
    }

    @Test
    void shouldRejectUnsafePrefixAndExcessivePartCount() {
        OssUploadProperties properties = validProperties();
        properties.getPolicies().get("general").setObjectPrefix("../outside");
        assertThrows(OssUploadException.class, properties::validate);

        properties = validProperties();
        properties.getPolicies().get("general").setMaxSize(60L * 1024 * 1024 * 1024);
        properties.getPolicies().get("general").setPartSize(OssUploadProperties.MIN_PART_SIZE);
        assertThrows(OssUploadException.class, properties::validate);

        properties = validProperties();
        properties.getPolicies().get("general").setMaxSize(Long.MAX_VALUE);
        assertThrows(OssUploadException.class, properties::validate);
    }

    static OssUploadProperties validProperties() {
        OssUploadProperties.Policy policy = new OssUploadProperties.Policy();
        policy.setMaxSize(2L * 1024 * 1024 * 1024);
        policy.setAllowedContentTypes(Set.of("image/png", "application/octet-stream"));
        policy.setObjectPrefix("direct/general");
        policy.setMode(OssUploadMode.AUTO);
        policy.setMultipartThreshold(100L * 1024 * 1024);
        policy.setPartSize(16L * 1024 * 1024);
        policy.setExpectedAccessPolicy(AccessPolicy.PRIVATE);
        OssUploadProperties properties = new OssUploadProperties();
        properties.setPolicies(Map.of("general", policy));
        return properties;
    }
}
