package org.namewta.test.oss.config;

import org.namewta.common.oss.config.AccessControlPolicyConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.exception.S3StorageException;
import org.namewta.common.oss.properties.OssProperties;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.upload.OssUploadContracts.InitRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OSS 公共只读与私有访问配置合同测试。
 */
@Tag("dev")
class OssAccessConfigurationUnitTest {

    @Test
    void shouldExposeOnlyPrivateAndPublicReadWithPrivateDefault() {
        assertArrayEquals(new AccessPolicy[]{AccessPolicy.PRIVATE, AccessPolicy.PUBLIC_READ}, AccessPolicy.values());
        assertEquals(AccessPolicy.PRIVATE, AccessControlPolicyConfig.DEFAULT.accessPolicy());

        OssProperties source = new OssProperties();
        assertEquals(AccessPolicy.PRIVATE,
            OssClientConfig.formProperties(source).accessControlPolicyConfig().accessPolicy());
        assertThrows(S3StorageException.class, () -> AccessPolicy.formType("1"));
    }

    @Test
    void shouldResolveBoundedServerNamedDownloadPolicies() {
        OssLifecycleProperties properties = new OssLifecycleProperties();
        OssLifecycleProperties.DownloadPolicy preview = new OssLifecycleProperties.DownloadPolicy();
        preview.setTtl(Duration.ofMinutes(5));
        properties.setDownloadPolicies(Map.of("preview", preview));

        assertDoesNotThrow(properties::validate);
        assertEquals(Duration.ofMinutes(2), properties.resolveDownloadTtl(null));
        assertEquals(Duration.ofMinutes(5), properties.resolveDownloadTtl("preview"));
        assertThrows(IllegalStateException.class, () -> properties.resolveDownloadTtl("missing"));
    }

    @Test
    void shouldRejectDisabledOrOutOfRangeDownloadPolicies() {
        OssLifecycleProperties disabledProperties = new OssLifecycleProperties();
        OssLifecycleProperties.DownloadPolicy disabled = new OssLifecycleProperties.DownloadPolicy();
        disabled.setEnabled(false);
        disabled.setTtl(Duration.ofMinutes(5));
        disabledProperties.setDownloadPolicies(Map.of("preview", disabled));
        assertDoesNotThrow(disabledProperties::validate);
        assertThrows(IllegalStateException.class, () -> disabledProperties.resolveDownloadTtl("preview"));

        OssLifecycleProperties.DownloadPolicy tooLong = new OssLifecycleProperties.DownloadPolicy();
        tooLong.setTtl(Duration.ofMinutes(11));
        OssLifecycleProperties tooLongProperties = new OssLifecycleProperties();
        tooLongProperties.setDownloadPolicies(Map.of("preview", tooLong));
        assertThrows(IllegalStateException.class, tooLongProperties::validate);

        OssLifecycleProperties invalidDefaultProperties = new OssLifecycleProperties();
        invalidDefaultProperties.setDownloadTtlMin(Duration.ofMinutes(3));
        assertThrows(IllegalStateException.class, invalidDefaultProperties::validate);
    }

    @Test
    void clientUploadRequestMustNotSelectStorageOrAccessLifetime() {
        Set<String> fields = Arrays.stream(InitRequest.class.getRecordComponents())
            .map(component -> component.getName()).collect(Collectors.toSet());

        assertEquals(Set.of("policy", "fileName", "fileSize", "contentType", "fingerprint"), fields);
        assertFalse(fields.contains("storageConfigKey"));
        assertFalse(fields.contains("expectedAccessPolicy"));
        assertFalse(fields.contains("ttl"));
    }
}
