package org.namewta.profile.person.domain.verification;

/** PersonVerificationCallbackOutcome 认证领域模型。 */
public enum PersonVerificationCallbackOutcome {
    ACCEPTED,
    IDEMPOTENT,
    LATE_IGNORED
}
