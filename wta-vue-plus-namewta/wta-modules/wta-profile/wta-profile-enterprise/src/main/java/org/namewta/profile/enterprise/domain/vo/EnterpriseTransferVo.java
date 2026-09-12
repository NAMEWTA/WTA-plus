package org.namewta.profile.enterprise.domain.vo;

/** EnterpriseTransferVo 对外返回模型。 */
public record EnterpriseTransferVo(String status, String challengeId, Long expiresInSeconds) {

        /** 创建已发送的转移结果。 */
        public static EnterpriseTransferVo sent(String challengeId) {
            return new EnterpriseTransferVo("SENT", challengeId, 300L);
        }

        /** 返回当前状态。 */
        public static EnterpriseTransferVo status(String status) {
            return new EnterpriseTransferVo(status, null, null);
        }
}
