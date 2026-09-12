package org.namewta.system.oss.readiness;

import org.namewta.common.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 保存最近一次 Provider 诊断的不可变快照。
 */
@Component
public class OssStorageReadinessRegistry {

    private final OssStorageReadinessProperties properties;
    private final Clock clock;
    private final AtomicReference<Snapshot> current = new AtomicReference<>(Snapshot.empty());

    @Autowired
    public OssStorageReadinessRegistry(OssStorageReadinessProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public OssStorageReadinessRegistry(OssStorageReadinessProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void replace(Map<String, OssStorageReadinessEntry> entries, Set<String> requiredKeys,
                        boolean discoverySucceeded) {
        current.set(new Snapshot(Map.copyOf(entries), Set.copyOf(requiredKeys), discoverySucceeded, clock.instant()));
    }

    public Map<String, OssStorageReadinessEntry> snapshot() {
        Snapshot snapshot = current.get();
        Map<String, OssStorageReadinessEntry> visible = new LinkedHashMap<>();
        snapshot.entries().forEach((key, value) -> visible.put(key, applyFreshness(value)));
        return Map.copyOf(visible);
    }

    public boolean overallServing() {
        Snapshot snapshot = current.get();
        if (!snapshot.discoverySucceeded()) {
            return false;
        }
        return snapshot.requiredKeys().stream()
            .map(snapshot.entries()::get)
            .allMatch(entry -> entry != null && applyFreshness(entry).status()
                == OssStorageReadinessEntry.Status.SERVING);
    }

    public boolean isServing(String configKey) {
        OssStorageReadinessEntry entry = current.get().entries().get(configKey);
        return entry != null && applyFreshness(entry).status() == OssStorageReadinessEntry.Status.SERVING;
    }

    public void requireServing(String configKey) {
        OssStorageReadinessEntry entry = current.get().entries().get(configKey);
        OssStorageReadinessEntry visible = entry == null ? null : applyFreshness(entry);
        if (visible == null || visible.status() != OssStorageReadinessEntry.Status.SERVING) {
            String reason = visible == null ? "MISSING" : visible.reason().name();
            throw new ServiceException("OSS存储配置当前不可服务: " + configKey + " (" + reason + ")");
        }
    }

    public boolean discoverySucceeded() {
        return current.get().discoverySucceeded();
    }

    private OssStorageReadinessEntry applyFreshness(OssStorageReadinessEntry entry) {
        Instant expiresAt = entry.checkedAt().plus(properties.getMaxSnapshotAge());
        return clock.instant().isAfter(expiresAt) ? entry.stale() : entry;
    }

    private record Snapshot(Map<String, OssStorageReadinessEntry> entries, Set<String> requiredKeys,
                            boolean discoverySucceeded, Instant refreshedAt) {
        static Snapshot empty() {
            return new Snapshot(Map.of(), Set.of(), false, Instant.EPOCH);
        }
    }
}
