package org.namewta.profile.enterprise.domain.vo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** EnterpriseProfileVersionVo 对外返回模型。 */
public record EnterpriseProfileVersionVo(
    long versionId,
    int versionNo,
    String sourceType,
    long sourceId,
    String enterpriseName,
    String unifiedCreditCode,
    String enterpriseType,
    String legalRepresentativeName,
    String legalDocumentTypeCode,
    String legalDocumentNumber,
    LocalDate establishedDate,
    LocalDate businessTermFrom,
    LocalDate businessTermUntil,
    String registeredAddress,
    String businessScope,
    String contactName,
    String contactPhone,
    String email,
    BigDecimal registeredCapital,
    String industryCode,
    String website,
    String status,
    Instant publishedTime
) {
}
