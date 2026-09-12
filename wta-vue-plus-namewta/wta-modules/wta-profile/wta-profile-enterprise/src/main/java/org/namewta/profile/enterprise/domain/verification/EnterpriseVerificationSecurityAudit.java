package org.namewta.profile.enterprise.domain.verification;

import java.time.Instant;
import java.util.Objects;

/** EnterpriseVerificationSecurityAudit 认证领域模型。 */
public record EnterpriseVerificationSecurityAudit(
    Long applicationId,
    EnterpriseVerificationFailureCategory category,
    Instant occurredAt
) {
    /** 校验企业认证安全审计记录。 */
    public EnterpriseVerificationSecurityAudit {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
