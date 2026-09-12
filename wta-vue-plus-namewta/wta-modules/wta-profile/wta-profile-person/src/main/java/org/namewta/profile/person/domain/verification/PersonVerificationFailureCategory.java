package org.namewta.profile.person.domain.verification;

/** PersonVerificationFailureCategory 认证领域模型。 */
public enum PersonVerificationFailureCategory {
    UNKNOWN_PROVIDER,
    DISABLED_PROVIDER,
    DUPLICATE_PROVIDER,
    INVALID_PROVIDER_CODE,
    UNSUPPORTED_CALLBACK,
    INVALID_SIGNATURE,
    EXPIRED_CALLBACK,
    CONFLICTING_CALLBACK,
    LATE_CALLBACK,
    ATTEMPT_NOT_FOUND,
    APPLICATION_NOT_FOUND,
    STALE_SUBMISSION,
    APPLICATION_TERMINAL,
    PROVIDER_MISMATCH,
    PROVIDER_FAILURE
}
