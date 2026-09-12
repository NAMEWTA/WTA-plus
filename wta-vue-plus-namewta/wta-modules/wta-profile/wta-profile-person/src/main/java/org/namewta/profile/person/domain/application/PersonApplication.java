package org.namewta.profile.person.domain.application;

import java.time.Instant;

/** PersonApplication 应用层领域模型。 */
public record PersonApplication(
    long personApplicationId,
    long applicantUserId,
    Long targetProfileId,
    String status,
    PersonIdentityFields fields,
    String providerCode,
    int submissionSeq,
    boolean rebindIntent,
    Long expectedBindingId,
    Integer expectedBindingVersion,
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
