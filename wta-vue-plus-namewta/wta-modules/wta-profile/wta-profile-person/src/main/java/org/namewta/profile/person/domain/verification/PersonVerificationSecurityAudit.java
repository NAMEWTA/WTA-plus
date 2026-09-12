package org.namewta.profile.person.domain.verification;

import java.time.Instant;
import java.util.Objects;

/** PersonVerificationSecurityAudit 认证领域模型。 */
public record PersonVerificationSecurityAudit(
    Long applicationId,
    PersonVerificationFailureCategory category,
    Instant occurredAt
) {
    /** 校验个人认证安全审计记录。 */
    public PersonVerificationSecurityAudit {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
