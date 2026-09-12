package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/** 个人申请提交快照查询读模型。 */
@Data
public class PersonSubmissionRow {

    private Long personSubmissionId;
    private Long personApplicationId;
    private Integer submissionSeq;
    private Long applicantUserId;
    private String fullName;
    private String documentTypeCode;
    private String documentNumber;
    private String identityKey;
    private String gender;
    private LocalDate birthDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String providerCode;
    private String rebindIntent;
    private Long targetProfileId;
    private Long expectedBindingId;
    private Integer expectedBindingVersion;
    private String fieldSnapshotJson;
    private Instant submittedTime;
}
