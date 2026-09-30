package org.namewta.profile.api.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * 个人档案当前有效版本的已核准投影；null 表示未授权或无对应资料。
 * <p>不包含材料、历史版本、申请与审核记录；完整证件与脱敏证件独立授权。</p>
 */
public record PersonDisclosure(
    Boolean verified,
    Long profileId,
    Instant verifiedAt,
    String fullName,
    String gender,
    LocalDate birthDate,
    String documentType,
    String documentNumberMasked,
    String documentNumber,
    LocalDate validFrom,
    LocalDate validUntil
) {
    /**
     * 再次按白名单裁剪，避免贡献者误返未授权字段。
     * @param fields 调用方已核定的字段集合
     * @return 只保留授权字段的不可变投影
     */
    public PersonDisclosure filtered(Set<ProfileDisclosureField> fields) {
        return new PersonDisclosure(
            fields.contains(ProfileDisclosureField.PERSON_VERIFIED) ? verified : null,
            fields.contains(ProfileDisclosureField.PERSON_PROFILE_ID) ? profileId : null,
            fields.contains(ProfileDisclosureField.PERSON_VERIFIED_AT) ? verifiedAt : null,
            fields.contains(ProfileDisclosureField.PERSON_FULL_NAME) ? fullName : null,
            fields.contains(ProfileDisclosureField.PERSON_GENDER) ? gender : null,
            fields.contains(ProfileDisclosureField.PERSON_BIRTH_DATE) ? birthDate : null,
            fields.contains(ProfileDisclosureField.PERSON_DOCUMENT_TYPE) ? documentType : null,
            fields.contains(ProfileDisclosureField.PERSON_DOCUMENT_NUMBER_MASKED) ? documentNumberMasked : null,
            fields.contains(ProfileDisclosureField.PERSON_DOCUMENT_NUMBER) ? documentNumber : null,
            fields.contains(ProfileDisclosureField.PERSON_VALID_FROM) ? validFrom : null,
            fields.contains(ProfileDisclosureField.PERSON_VALID_UNTIL) ? validUntil : null
        );
    }
}

