package org.namewta.profile.person.domain.verification;

import java.util.Objects;

/** PersonProviderStartCommand 认证领域模型。 */
public record PersonProviderStartCommand(
    long applicationId,
    long submissionId,
    int attemptNo,
    String requestFingerprint
) {
    /** 校验个人认证提供方启动命令。 */
    public PersonProviderStartCommand {
        if (applicationId <= 0 || submissionId <= 0 || attemptNo <= 0) {
            throw new IllegalArgumentException("applicationId, submissionId and attemptNo must be positive");
        }
        Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        if (requestFingerprint.isBlank()) {
            throw new IllegalArgumentException("requestFingerprint must not be blank");
        }
    }
}
