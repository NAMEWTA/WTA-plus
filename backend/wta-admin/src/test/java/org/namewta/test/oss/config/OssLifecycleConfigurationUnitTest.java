package org.namewta.test.oss.config;

import org.namewta.system.oss.config.OssLifecycleProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 私有下载策略的 Spring 配置绑定与启动校验测试。
 */
@Tag("dev")
class OssLifecycleConfigurationUnitTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
            "oss.lifecycle.download-ttl=2m",
            "oss.lifecycle.download-ttl-min=1m",
            "oss.lifecycle.download-ttl-max=10m",
            "oss.lifecycle.download-policies.preview.ttl=5m");

    @Test
    void shouldBindValidNamedPolicy() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            OssLifecycleProperties properties = context.getBean(OssLifecycleProperties.class);
            assertEquals(Duration.ofMinutes(2), properties.resolveDownloadTtl(null));
            assertEquals(Duration.ofMinutes(5), properties.resolveDownloadTtl("preview"));
        });
    }

    @Test
    void shouldFailContextForOutOfRangeNamedPolicy() {
        runner.withPropertyValues("oss.lifecycle.download-policies.preview.ttl=11m")
            .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void shouldFailContextWhenDefaultIsOutsideConfiguredBounds() {
        runner.withPropertyValues("oss.lifecycle.download-ttl-min=3m")
            .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OssLifecycleProperties.class)
    static class PropertiesConfiguration {
    }
}
