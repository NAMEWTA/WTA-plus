package org.namewta.system.oss.readiness;

import org.namewta.common.oss.enums.AccessPolicy;

import java.time.Instant;
import java.util.Set;

/**
 * 单个 configKey 的不可变服务状态。
 */
public record OssStorageReadinessEntry(
    String configKey,
    AccessPolicy accessPolicy,
    boolean required,
    Set<String> requiredBy,
    Status status,
    Reason reason,
    Instant checkedAt
) {

    public OssStorageReadinessEntry {
        requiredBy = requiredBy == null ? Set.of() : Set.copyOf(requiredBy);
    }

    public OssStorageReadinessEntry stale() {
        return new OssStorageReadinessEntry(configKey, accessPolicy, required, requiredBy,
            Status.NOT_SERVING, Reason.STALE, checkedAt);
    }

    public enum Status {
        SERVING,
        NOT_SERVING
    }

    public enum Reason {
        READY,
        CONFIG_MISSING,
        INVALID_ACCESS_POLICY,
        DOMAIN_REQUIRED,
        DIAGNOSTIC_OBJECT_MISSING,
        DIAGNOSTIC_UNVERIFIED,
        PROVIDER_MISMATCH,
        DISCOVERY_FAILED,
        STALE
    }
}
