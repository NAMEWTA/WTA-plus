package org.namewta.profile.person.domain.verification;

import java.time.Instant;
import java.util.Objects;

/** PersonVerifiedCallback 认证领域模型。 */
public record PersonVerifiedCallback(
    String providerRequestId,
    String callbackDigest,
    PersonProviderAttemptStatus status,
    String normalizedResultJson,
    String providerEvidenceJson,
    String errorCode,
    Instant completedAt
) {
    /** 校验个人认证回调的关键字段。 */
    public PersonVerifiedCallback {
        Objects.requireNonNull(providerRequestId, "providerRequestId");
        Objects.requireNonNull(callbackDigest, "callbackDigest");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}
