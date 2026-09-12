package org.namewta.profile.enterprise.port.store;

import org.namewta.profile.enterprise.domain.transfer.EnterpriseTransferChallenge;
/** 企业转移挑战存储接口，定义暂存、验证、消费和撤销生命周期。 */
public interface EnterpriseTransferChallengeStore {

    /** 暂存转移挑战并返回暂存结果。 */
    StageResult stage(EnterpriseTransferChallenge challenge);

    /** 激活转移挑战。 */
    boolean activate(String challengeId);

    /** 核验转移挑战并返回验证结果。 */
    Verification verify(String challengeId, long sourceUserId, String code);

    /** 消费并移除已验证的转移挑战。 */
    boolean consume(VerifiedChallenge verifiedChallenge);

    /** 撤销转移挑战或档案绑定。 */
    void revoke(String challengeId);

    /** 企业转移挑战暂存结果。 */
    enum StageResult {
        STAGED,
        RATE_LIMITED
    }

    /** 企业转移挑战验证状态。 */
    enum VerificationStatus {
        VERIFIED,
        INVALID
    }

    /** 企业转移挑战验证结果。 */
    record Verification(VerificationStatus status, VerifiedChallenge verified) {
    }

    /** 已通过验证且待消费的企业转移挑战。 */
    record VerifiedChallenge(EnterpriseTransferChallenge challenge, String storageToken) {
    }
}
