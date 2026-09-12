package org.namewta.profile.enterprise.domain.verification;

import java.util.Objects;

/** EnterpriseVerificationStartAttemptCommand 认证领域模型。 */
public record EnterpriseVerificationStartAttemptCommand(
    long applicationId,
    long submissionId,
    String requestFingerprint
) {
    /** 校验企业认证启动命令的编号和指纹。 */
    public EnterpriseVerificationStartAttemptCommand {
        if (applicationId <= 0 || submissionId <= 0) {
            throw new IllegalArgumentException("applicationId and submissionId must be positive");
        }
        Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        if (requestFingerprint.isBlank()) {
            throw new IllegalArgumentException("requestFingerprint must not be blank");
        }
    }
}
