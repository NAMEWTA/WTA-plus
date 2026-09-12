package org.namewta.system.oss.readiness;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对外暴露不含凭据、Policy 和 URL 的 OSS readiness 详情。
 */
@Component("ossStorageReadiness")
@RequiredArgsConstructor
public class OssStorageReadinessHealthIndicator implements HealthIndicator {

    private final OssStorageReadinessRegistry registry;

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        registry.snapshot().forEach((configKey, entry) -> details.put(configKey, Map.of(
            "status", entry.status().name(),
            "accessPolicy", entry.accessPolicy() == null ? "UNKNOWN" : entry.accessPolicy().name(),
            "required", entry.required(),
            "requiredBy", entry.requiredBy(),
            "reason", entry.reason().name(),
            "checkedAt", entry.checkedAt().toString()
        )));
        Health.Builder builder = registry.overallServing() ? Health.up() : Health.down();
        return builder.withDetail("discoverySucceeded", registry.discoverySucceeded())
            .withDetail("configs", details).build();
    }
}
