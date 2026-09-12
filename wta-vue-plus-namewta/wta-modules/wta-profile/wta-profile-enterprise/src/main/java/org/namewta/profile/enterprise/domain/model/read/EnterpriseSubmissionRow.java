package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** 企业申请提交快照查询读模型。 */
@Data
public class EnterpriseSubmissionRow {
    private Long enterpriseSubmissionId;
    private Long enterpriseApplicationId;
    private Integer submissionSeq;
    private Long applicantUserId;
    private String enterpriseName;
    private String unifiedCreditCode;
    private String identityKey;
    private String enterpriseType;
    private String legalRepresentativeName;
    private String legalDocumentTypeCode;
    private String legalDocumentNumber;
    private String handlerIsLegalRepresentative;
    private LocalDate establishedDate;
    private LocalDate businessTermFrom;
    private LocalDate businessTermUntil;
    private String registeredAddress;
    private String businessScope;
    private String contactName;
    private String contactPhone;
    private String email;
    private BigDecimal registeredCapital;
    private String industryCode;
    private String website;
    private String providerCode;
    private Long targetProfileId;
    private String fieldSnapshotJson;
    private Instant submittedTime;
}
