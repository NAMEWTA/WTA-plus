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
    // 粗粒度进程内版本：宁可让并发诊断重试，也不发布早于配置事件的结果。
    private long configRevision;

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

    /** 配置提交后仅使该 key 的诊断事实失效；不触发远端探测。 */
    public synchronized void invalidate(String configKey) {
        if (configKey == null) {
            return;
        }
        configRevision++;
        current.updateAndGet(previous -> {
            Map<String, OssStorageReadinessEntry> entries = new LinkedHashMap<>(previous.entries());
            entries.remove(configKey);
            return new Snapshot(Map.copyOf(entries), previous.requiredKeys(),
                previous.discoverySucceeded(), previous.refreshedAt());
        });
    }

    /** 开始读取 DB 前取版本，防止诊断期间配置提交后旧结果重新生效。 */
    public synchronized long configRevision() {
        return configRevision;
    }

    /** 仅当配置事件未发生时合并观察结果；网络 I/O 不持此锁。 */
    public synchronized boolean recordIfUnchanged(OssStorageReadinessEntry entry, long revision) {
        if (revision != configRevision) {
            return false;
        }
        configRevision++;
        current.updateAndGet(previous -> {
            Map<String, OssStorageReadinessEntry> entries = new LinkedHashMap<>(previous.entries());
            entries.put(entry.configKey(), new OssStorageReadinessEntry(entry.configKey(),
                entry.accessPolicy(), true, Set.of("MANUAL_DIAGNOSIS"), entry.status(),
                entry.reason(), entry.checkedAt()));
            Set<String> required = new java.util.LinkedHashSet<>(previous.requiredKeys());
            required.add(entry.configKey());
            return new Snapshot(Map.copyOf(entries), Set.copyOf(required), true, clock.instant());
        });
        return true;
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
        Instant expiresAt = entry.checkedAt().plus(properties.resolvedMaxSnapshotAge());
        return clock.instant().isAfter(expiresAt) ? entry.stale() : entry;
    }

    private record Snapshot(Map<String, OssStorageReadinessEntry> entries, Set<String> requiredKeys,
                            boolean discoverySucceeded, Instant refreshedAt) {
        static Snapshot empty() {
            return new Snapshot(Map.of(), Set.of(), false, Instant.EPOCH);
        }
    }
}
