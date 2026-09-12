package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/** 个人申请查询读模型。 */
@Data
public class PersonApplicationRow {

    private Long personApplicationId;
    private Long applicantUserId;
    private Long targetProfileId;
    private String status;
    private String fullName;
    private String documentTypeCode;
    private String documentNumber;
    private String identityKey;
    private String gender;
    private LocalDate birthDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String providerCode;
    private Integer submissionSeq;
    private String rebindIntent;
    private Long expectedBindingId;
    private Integer expectedBindingVersion;
    private Integer decisionVersion;
    private Integer version;
    private Instant submittedTime;
    private Instant finishedTime;
}
