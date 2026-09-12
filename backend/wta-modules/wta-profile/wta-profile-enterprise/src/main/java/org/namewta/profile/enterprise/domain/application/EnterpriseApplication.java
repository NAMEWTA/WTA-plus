package org.namewta.profile.enterprise.domain.application;

import java.time.Instant;

/** EnterpriseApplication 应用层领域模型。 */
public record EnterpriseApplication(
    long enterpriseApplicationId,
    long applicantUserId,
    Long targetProfileId,
    String status,
    EnterpriseIdentityFields fields,
    String providerCode,
    int submissionSeq,
    int decisionVersion,
    int version,
    Instant submittedTime,
    Instant finishedTime
) {

    /** 判断申请是否允许编辑。 */
    public boolean editable() {
        return "DRAFT".equals(status) || "BACK".equals(status) || "CANCEL".equals(status);
    }

    /** 判断申请是否处于终态。 */
    public boolean terminal() {
        return "FINISH".equals(status) || "INVALID".equals(status) || "TERMINATION".equals(status);
    }
}
