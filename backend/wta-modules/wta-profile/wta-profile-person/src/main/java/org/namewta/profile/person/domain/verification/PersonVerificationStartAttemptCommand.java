package org.namewta.profile.person.domain.verification;

import java.util.Objects;

/** PersonVerificationStartAttemptCommand 认证领域模型。 */
public record PersonVerificationStartAttemptCommand(
    long applicationId,
    long submissionId,
    String requestFingerprint
) {
    /** 校验个人认证启动命令的编号和指纹。 */
    public PersonVerificationStartAttemptCommand {
        if (applicationId <= 0 || submissionId <= 0) {
            throw new IllegalArgumentException("applicationId and submissionId must be positive");
        }
        Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        if (requestFingerprint.isBlank()) {
            throw new IllegalArgumentException("requestFingerprint must not be blank");
        }
    }
}
