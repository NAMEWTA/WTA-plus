package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.Instant;

/** 个人认证尝试记录查询读模型。 */
@Data
public class PersonVerificationAttemptRow {

    private Long verificationAttemptId;
    private Long applicationId;
    private Long submissionId;
    private String providerCode;
    private String providerRequestId;
    private String requestFingerprint;
    private Integer attemptNo;
    private String status;
    private String normalizedResultJson;
    private String providerEvidenceJson;
    private String errorCode;
    private Instant completedTime;
}
