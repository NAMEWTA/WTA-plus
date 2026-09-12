package org.namewta.profile.enterprise.domain.application;

import java.time.Instant;

/** EnterpriseSubmission 应用层领域模型。 */
public record EnterpriseSubmission(
    long enterpriseSubmissionId,
    long enterpriseApplicationId,
    int submissionSeq,
    long applicantUserId,
    EnterpriseIdentityFields fields,
    String providerCode,
    Instant submittedTime
) {
}
