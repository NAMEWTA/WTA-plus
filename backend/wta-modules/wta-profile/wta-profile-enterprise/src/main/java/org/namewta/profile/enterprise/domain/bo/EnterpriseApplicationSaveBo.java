package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** EnterpriseApplicationSaveBo 请求参数模型。 */
public record EnterpriseApplicationSaveBo(
    @Size(max = 255) String enterpriseName,
    @Size(max = 64) String unifiedCreditCode,
    @Size(max = 64) String enterpriseType,
    @Size(max = 100) String legalRepresentativeName,
    @Size(max = 64) String legalDocumentTypeCode,
    @Size(max = 128) String legalDocumentNumber,
    boolean handlerIsLegalRepresentative,
    LocalDate establishedDate,
    LocalDate businessTermFrom,
    LocalDate businessTermUntil,
    @Size(max = 500) String registeredAddress,
    String businessScope,
    @Size(max = 100) String contactName,
    @Size(max = 64) String contactPhone,
    @Size(max = 255) String email,
    @PositiveOrZero BigDecimal registeredCapital,
    @Size(max = 64) String industryCode,
    @Size(max = 500) String website,
    @PositiveOrZero int expectedVersion
) {
}
