package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** 企业申请查询读模型。 */
@Data
public class EnterpriseApplicationRow {
    private Long enterpriseApplicationId;
    private Long applicantUserId;
    private Long targetProfileId;
    private String status;
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
    private Integer submissionSeq;
    private Integer decisionVersion;
    private Integer version;
    private Instant submittedTime;
    private Instant finishedTime;
}
