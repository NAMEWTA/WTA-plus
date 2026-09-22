package org.namewta.test.oss.upload;

import org.namewta.system.oss.upload.OssUploadProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * uploadPolicy 配置绑定和启动失败测试。
 */
@Tag("dev")
class OssUploadConfigurationUnitTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
            "oss.direct-upload.policies.general.max-size=2147483648",
            "oss.direct-upload.policies.general.expected-access-policy=PRIVATE",
            "oss.direct-upload.policies.general.allowed-content-types[0]=image/png",
            "oss.direct-upload.policies.general.object-prefix=direct/general",
            "oss.direct-upload.policies.general.mode=AUTO",
            "oss.direct-upload.policies.general.multipart-threshold=104857600",
            "oss.direct-upload.policies.general.part-size=16777216");

    @Test
    void shouldBindValidPolicyWithFiveMinutePresignDefault() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            OssUploadProperties properties = context.getBean(OssUploadProperties.class);
            assertEquals(5, properties.getPresignTtl().toMinutes());
            assertTrue(properties.requirePolicy("general").allowsContentType("image/png"));
        });
    }

    @Test
    void shouldFailContextForUnsafeObjectPrefix() {
        runner.withPropertyValues("oss.direct-upload.policies.general.object-prefix=../escape")
            .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void shouldFailContextForUnsupportedAccessPolicy() {
        runner.withPropertyValues("oss.direct-upload.policies.general.expected-access-policy=PUBLIC_READ_WRITE")
            .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void shouldStartWhenPolicyDoesNotNameAStorageConfig() {
        new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                "oss.direct-upload.policies.general.max-size=2147483648",
                "oss.direct-upload.policies.general.expected-access-policy=PRIVATE",
                "oss.direct-upload.policies.general.allowed-content-types[0]=image/png",
                "oss.direct-upload.policies.general.object-prefix=direct/general",
                "oss.direct-upload.policies.general.mode=AUTO",
                "oss.direct-upload.policies.general.multipart-threshold=104857600",
                "oss.direct-upload.policies.general.part-size=16777216")
            .run(context -> assertNull(context.getStartupFailure()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OssUploadProperties.class)
    static class PropertiesConfiguration {
    }
}
