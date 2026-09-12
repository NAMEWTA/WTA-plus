package org.namewta.profile.enterprise.domain.verification;

import java.time.Instant;
import java.util.Objects;

/** EnterpriseVerifiedCallback 认证领域模型。 */
public record EnterpriseVerifiedCallback(
    String providerRequestId,
    String callbackDigest,
    EnterpriseProviderAttemptStatus status,
    String normalizedResultJson,
    String providerEvidenceJson,
    String errorCode,
    Instant completedAt
) {
    /** 校验企业认证回调的关键字段。 */
    public EnterpriseVerifiedCallback {
        Objects.requireNonNull(providerRequestId, "providerRequestId");
        Objects.requireNonNull(callbackDigest, "callbackDigest");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(completedAt, "completedAt");
    }
}
