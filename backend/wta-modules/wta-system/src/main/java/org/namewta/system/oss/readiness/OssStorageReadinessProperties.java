package org.namewta.system.oss.readiness;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * OSS Provider 只读诊断配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss.readiness")
public class OssStorageReadinessProperties implements InitializingBean {

    private static final Pattern CONFIG_KEY = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9._-]{1,19}");

    private Duration diagnosticTimeout = Duration.ofSeconds(3);
    private Duration refreshInterval = Duration.ofMinutes(1);
    private Duration maxSnapshotAge = Duration.ofMinutes(5);
    private boolean allowEndpointDomainFallback;
    private Map<String, String> diagnosticObjects = new LinkedHashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (!within(diagnosticTimeout, Duration.ofMillis(100), Duration.ofSeconds(30))) {
            throw new IllegalStateException("OSS readiness diagnosticTimeout 超出安全范围");
        }
        if (!within(maxSnapshotAge, diagnosticTimeout, Duration.ofHours(1))) {
            throw new IllegalStateException("OSS readiness maxSnapshotAge 超出安全范围");
        }
        if (!within(refreshInterval, Duration.ofMillis(100), Duration.ofHours(1))
            || refreshInterval.compareTo(maxSnapshotAge) >= 0) {
            throw new IllegalStateException("OSS readiness refreshInterval 必须小于 maxSnapshotAge");
        }
        if (diagnosticObjects == null) {
            throw new IllegalStateException("OSS readiness diagnosticObjects 不能为空");
        }
        diagnosticObjects.forEach((configKey, objectKey) -> {
            if (configKey == null || !CONFIG_KEY.matcher(configKey).matches() || objectKey == null
                || objectKey.isBlank() || objectKey.startsWith("/") || objectKey.contains("..")) {
                throw new IllegalStateException("OSS readiness 诊断对象配置无效: " + configKey);
            }
        });
    }

    private boolean within(Duration value, Duration min, Duration max) {
        return value != null && !value.isNegative() && !value.isZero()
            && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
