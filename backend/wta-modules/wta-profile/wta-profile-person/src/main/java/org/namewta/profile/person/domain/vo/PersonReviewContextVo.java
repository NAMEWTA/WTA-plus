package org.namewta.profile.person.domain.vo;

import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;

import java.time.Instant;
import java.util.List;

/** PersonReviewContextVo 对外返回模型。 */
public record PersonReviewContextVo(
    long applicationId,
    long applicantUserId,
    String status,
    int submissionSeq,
    int decisionVersion,
    int version,
    long submissionId,
    String fieldSnapshotJson,
    Instant submittedTime,
    List<MaterialReferenceView> materials
) {
}
