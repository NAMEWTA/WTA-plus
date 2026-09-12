package org.namewta.profile.person.domain.application;

import java.time.Instant;

/** PersonSubmission 应用层领域模型。 */
public record PersonSubmission(
    long personSubmissionId,
    long personApplicationId,
    int submissionSeq,
    long applicantUserId,
    PersonIdentityFields fields,
    String providerCode,
    Instant submittedTime
) {
}
