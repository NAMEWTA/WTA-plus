package org.namewta.profile.api.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Set;

/**
 * 企业档案当前有效版本的已核准投影；null 表示未授权或无对应资料。
 * <p>不包含材料、历史版本、申请与审核记录；完整证件与脱敏证件独立授权。</p>
 */
public record EnterpriseDisclosure(
    Boolean verified,
    Long profileId,
    Instant verifiedAt,
    String name,
    String creditCode,
    String type,
    String legalRepresentativeName,
    String legalDocumentType,
    String legalDocumentNumberMasked,
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
    /**
     * 再次按白名单裁剪，避免贡献者误返未授权字段。
     * @param fields 调用方已核定的字段集合
     * @return 只保留授权字段的不可变投影
     */
    public EnterpriseDisclosure filtered(Set<ProfileDisclosureField> fields) {
        return new EnterpriseDisclosure(
            fields.contains(ProfileDisclosureField.ENTERPRISE_VERIFIED) ? verified : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_PROFILE_ID) ? profileId : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_VERIFIED_AT) ? verifiedAt : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_NAME) ? name : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_CREDIT_CODE) ? creditCode : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_TYPE) ? type : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_LEGAL_REPRESENTATIVE_NAME) ? legalRepresentativeName : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_LEGAL_DOCUMENT_TYPE) ? legalDocumentType : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_LEGAL_DOCUMENT_NUMBER_MASKED) ? legalDocumentNumberMasked : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_LEGAL_DOCUMENT_NUMBER) ? legalDocumentNumber : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_ESTABLISHED_DATE) ? establishedDate : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_BUSINESS_TERM_FROM) ? businessTermFrom : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_BUSINESS_TERM_UNTIL) ? businessTermUntil : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_REGISTERED_ADDRESS) ? registeredAddress : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_BUSINESS_SCOPE) ? businessScope : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_CONTACT_NAME) ? contactName : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_CONTACT_PHONE) ? contactPhone : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_EMAIL) ? email : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_REGISTERED_CAPITAL) ? registeredCapital : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_INDUSTRY_CODE) ? industryCode : null,
            fields.contains(ProfileDisclosureField.ENTERPRISE_WEBSITE) ? website : null
        );
    }
}

