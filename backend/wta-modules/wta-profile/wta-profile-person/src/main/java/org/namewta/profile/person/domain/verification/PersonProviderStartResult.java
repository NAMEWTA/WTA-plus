package org.namewta.profile.person.domain.verification;

import java.time.Instant;
import java.util.Objects;

/** PersonProviderStartResult 认证领域模型。 */
public record PersonProviderStartResult(
    String providerRequestId,
    PersonProviderAttemptStatus status,
    String normalizedResultJson,
    String providerEvidenceJson,
    String errorCode,
    Instant completedAt
) {
    /** 校验个人认证提供方启动结果的状态约束。 */
    public PersonProviderStartResult {
        Objects.requireNonNull(status, "status");
        if ((status == PersonProviderAttemptStatus.PENDING) != (completedAt == null)) {
            throw new IllegalArgumentException("Only pending person attempts may omit completedAt");
        }
    }

    /** 创建待处理的认证启动结果。 */
    public static PersonProviderStartResult pending() {
        return new PersonProviderStartResult(
            null, PersonProviderAttemptStatus.PENDING, null, null, null, null);
    }
}
