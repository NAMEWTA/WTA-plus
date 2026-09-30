package org.namewta.profile.enterprise.domain.vo;

import java.time.LocalDate;
import java.math.BigDecimal;

/** 本人已认证身份资料，不包含内部身份索引或管理信息。 */
public record EnterpriseSelfIdentityVo(
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
    String website
) {
}
