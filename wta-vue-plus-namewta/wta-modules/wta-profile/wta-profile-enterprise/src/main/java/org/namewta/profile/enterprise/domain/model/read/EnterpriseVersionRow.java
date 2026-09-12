package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** 企业档案版本查询读模型。 */
@Data
public class EnterpriseVersionRow {
    private Long enterpriseVersionId;
    private Long enterpriseProfileId;
    private Integer versionNo;
    private String sourceType;
    private Long sourceId;
    private String enterpriseName;
    private String unifiedCreditCode;
    private String enterpriseType;
    private String legalRepresentativeName;
    private String legalDocumentTypeCode;
    private String legalDocumentNumber;
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
    private String status;
    private Instant publishedTime;
}
