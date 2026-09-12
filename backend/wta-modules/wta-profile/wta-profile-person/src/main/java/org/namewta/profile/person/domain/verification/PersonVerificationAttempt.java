package org.namewta.profile.person.domain.verification;

import java.time.Instant;
import java.util.Objects;

/** PersonVerificationAttempt 认证领域模型。 */
public record PersonVerificationAttempt(
    long verificationAttemptId,
    long applicationId,
    long submissionId,
    String providerCode,
    String providerRequestId,
    String requestFingerprint,
    String callbackDigest,
    int attemptNo,
    PersonProviderAttemptStatus status,
    String normalizedResultJson,
    String providerEvidenceJson,
    String errorCode,
    Instant completedAt
) {
    /** 校验个人认证尝试的标识和完成状态。 */
    public PersonVerificationAttempt {
        if (applicationId <= 0 || submissionId <= 0 || attemptNo <= 0) {
            throw new IllegalArgumentException("Attempt identifiers and attemptNo must be positive");
        }
        Objects.requireNonNull(providerCode, "providerCode");
        Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(status, "status");
    }

    /** 根据提供方启动结果创建认证尝试。 */
    public static PersonVerificationAttempt fromStart(PersonApplicationVerificationState application,
                                                      int attemptNo,
                                                      String requestFingerprint,
                                                      PersonProviderStartResult result) {
        return new PersonVerificationAttempt(
            0L,
            application.applicationId(),
            application.submissionId(),
            application.providerCode(),
            result.providerRequestId(),
            requestFingerprint,
            null,
            attemptNo,
            result.status(),
            result.normalizedResultJson(),
            result.providerEvidenceJson(),
            result.errorCode(),
            result.completedAt());
    }

    /** 判断回调内容是否重复。 */
    public boolean sameCallback(PersonVerifiedCallback callback) {
        return Objects.equals(callbackDigest, callback.callbackDigest())
            && status == callback.status()
            && Objects.equals(normalizedResultJson, callback.normalizedResultJson())
            && Objects.equals(providerEvidenceJson, callback.providerEvidenceJson())
            && Objects.equals(errorCode, callback.errorCode());
    }
}
