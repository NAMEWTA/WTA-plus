package org.namewta.profile.enterprise.domain.verification;

import java.util.Objects;

/** EnterpriseProviderStartCommand 认证领域模型。 */
public record EnterpriseProviderStartCommand(
    long applicationId,
    long submissionId,
    int attemptNo,
    String requestFingerprint
) {
    /** 校验企业认证提供方启动命令。 */
    public EnterpriseProviderStartCommand {
        if (applicationId <= 0 || submissionId <= 0 || attemptNo <= 0) {
            throw new IllegalArgumentException("applicationId, submissionId and attemptNo must be positive");
        }
        Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        if (requestFingerprint.isBlank()) {
            throw new IllegalArgumentException("requestFingerprint must not be blank");
        }
    }
}
