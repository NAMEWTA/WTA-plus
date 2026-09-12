package org.namewta.profile.enterprise.domain.transfer;

/** EnterpriseTransferChallenge 转移领域模型。 */
public record EnterpriseTransferChallenge(
    String challengeId,
    long sourceUserId,
    long targetUserId,
    long enterpriseProfileId,
    long sourceBindingId,
    int sourceBindingVersion,
    long personProfileId,
    String fullName,
    String documentLastFour,
    String phone,
    String codeHash,
    State state,
    int attempts,
    long expiresAtEpochMilli
) {

    /** State 转移领域模型。 */
    public enum State {
        PENDING_DELIVERY,
        ACTIVE
    }

    /** 激活转移挑战。 */
    public EnterpriseTransferChallenge activate() {
        return with(State.ACTIVE, attempts);
    }

    /** 记录认证失败尝试。 */
    public EnterpriseTransferChallenge failedAttempt() {
        return with(state, attempts + 1);
    }

    /** 创建替换字段后的挑战对象。 */
    private EnterpriseTransferChallenge with(State nextState, int nextAttempts) {
        return new EnterpriseTransferChallenge(challengeId, sourceUserId, targetUserId, enterpriseProfileId,
            sourceBindingId, sourceBindingVersion, personProfileId, fullName, documentLastFour, phone,
            codeHash, nextState, nextAttempts, expiresAtEpochMilli);
    }
}
