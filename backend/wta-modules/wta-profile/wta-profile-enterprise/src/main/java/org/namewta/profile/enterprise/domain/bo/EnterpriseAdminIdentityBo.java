package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** EnterpriseAdminIdentityBo 请求参数模型。 */
public record EnterpriseAdminIdentityBo(
    @NotBlank @Size(max = 255) String enterpriseName,
    @NotBlank @Size(max = 64) String unifiedCreditCode,
    @NotBlank @Size(max = 64) String enterpriseType,
    @NotBlank @Size(max = 100) String legalRepresentativeName,
    @NotBlank @Size(max = 64) String legalDocumentTypeCode,
    @NotBlank @Size(max = 128) String legalDocumentNumber,
    @NotNull LocalDate establishedDate,
    LocalDate businessTermFrom,
    LocalDate businessTermUntil,
    @NotBlank @Size(max = 500) String registeredAddress,
    @NotBlank String businessScope,
    @Size(max = 100) String contactName,
    @Size(max = 64) String contactPhone,
    @Size(max = 255) String email,
    @PositiveOrZero BigDecimal registeredCapital,
    @Size(max = 64) String industryCode,
    @Size(max = 500) String website
) {
}
