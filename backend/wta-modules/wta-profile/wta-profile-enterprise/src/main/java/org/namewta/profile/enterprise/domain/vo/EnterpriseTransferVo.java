package org.namewta.profile.enterprise.domain.vo;

/** EnterpriseTransferVo 对外返回模型。 */
public record EnterpriseTransferVo(String status, String challengeId, Long expiresInSeconds) {

        /** 创建等待通知受理的挑战结果；剩余期限不会因重查而延长。 */
        public static EnterpriseTransferVo queued(String challengeId, long expiresInSeconds) {
            return new EnterpriseTransferVo("QUEUED", challengeId, expiresInSeconds);
        }

        /** 返回当前状态。 */
        public static EnterpriseTransferVo status(String status) {
            return new EnterpriseTransferVo(status, null, null);
        }
}
