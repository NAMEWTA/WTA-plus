package org.namewta.profile.enterprise.domain.verification;

import java.time.Instant;
import java.util.Objects;

/** EnterpriseProviderStartResult 认证领域模型。 */
public record EnterpriseProviderStartResult(
    String providerRequestId,
    EnterpriseProviderAttemptStatus status,
    String normalizedResultJson,
    String providerEvidenceJson,
    String errorCode,
    Instant completedAt
) {
    /** 校验企业认证提供方启动结果的状态约束。 */
    public EnterpriseProviderStartResult {
        Objects.requireNonNull(status, "status");
        if ((status == EnterpriseProviderAttemptStatus.PENDING) != (completedAt == null)) {
            throw new IllegalArgumentException("Only pending enterprise attempts may omit completedAt");
        }
    }

    /** 创建待处理的认证启动结果。 */
    public static EnterpriseProviderStartResult pending() {
        return new EnterpriseProviderStartResult(
            null, EnterpriseProviderAttemptStatus.PENDING, null, null, null, null);
    }
}
