package org.namewta.system.oss.readiness;

import lombok.Data;
import org.springframework.boot.convert.DurationStyle;
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

    // 原始字符串由 Binder 接收；非法的可选诊断配置不能阻止核心应用启动。
    private String diagnosticTimeout = "PT3S";
    private String refreshInterval = "PT1M";
    private String maxSnapshotAge = "PT5M";
    private boolean allowEndpointDomainFallback;
    private Map<String, String> diagnosticObjects = new LinkedHashMap<>();

    @Override
    public void afterPropertiesSet() {
        // 此配置只用于手动诊断；校验由单配置诊断返回固定原因，不中止启动。
    }

    /** 每个网络步骤的超时。最多五个顺序步骤，因此网络等待上界为十五秒。 */
    public Duration boundedDiagnosticTimeout() {
        Duration parsed = parse(diagnosticTimeout);
        return within(parsed, Duration.ofMillis(100), Duration.ofSeconds(3)) ? parsed : null;
    }

    /** 无效可选快照时长只影响诊断显示，不能让健康端点抛错。 */
    public Duration resolvedMaxSnapshotAge() {
        Duration parsed = parse(maxSnapshotAge);
        return within(parsed, Duration.ofMillis(100), Duration.ofHours(1))
            ? parsed : Duration.ofMinutes(5);
    }

    /** 仅检查诊断自身配置，不构造客户端或执行网络 I/O。 */
    public boolean diagnosticConfigurationValid() {
        Duration refresh = parse(refreshInterval);
        Duration maxAge = parse(maxSnapshotAge);
        if (boundedDiagnosticTimeout() == null || diagnosticObjects == null
            || !within(maxAge, Duration.ofMillis(100), Duration.ofHours(1))
            || !within(refresh, Duration.ofMillis(100), Duration.ofHours(1))
            || refresh.compareTo(maxAge) >= 0) {
            return false;
        }
        return diagnosticObjects.entrySet().stream().allMatch(entry -> entry.getKey() != null
            && CONFIG_KEY.matcher(entry.getKey()).matches() && entry.getValue() != null
            && !entry.getValue().isBlank() && !entry.getValue().startsWith("/")
            && !entry.getValue().contains(".."));
    }

    private Duration parse(String value) {
        try {
            return value == null ? null : DurationStyle.detectAndParse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean within(Duration value, Duration min, Duration max) {
        return value != null && !value.isNegative() && !value.isZero()
            && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
