package org.namewta.profile.person.domain.verification;

import java.util.Objects;
import java.util.Set;

/** PersonApplicationVerificationState 认证领域模型。 */
public record PersonApplicationVerificationState(
    long applicationId,
    long submissionId,
    String providerCode,
    String status
) {
    private static final Set<String> TERMINAL_STATUSES = Set.of("FINISH", "INVALID", "TERMINATION");

    /** 校验个人申请认证状态的编号约束。 */
    public PersonApplicationVerificationState {
        if (applicationId <= 0 || submissionId <= 0) {
            throw new IllegalArgumentException("applicationId and submissionId must be positive");
        }
        Objects.requireNonNull(providerCode, "providerCode");
        Objects.requireNonNull(status, "status");
    }

    /** 判断申请是否处于终态。 */
    public boolean terminal() {
        return TERMINAL_STATUSES.contains(status);
    }
}
