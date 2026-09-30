package org.namewta.profile.person.domain.vo;

import java.time.LocalDate;

/** 本人已认证身份资料，不包含内部身份索引或管理信息。 */
public record PersonSelfIdentityVo(
    String fullName,
    String documentTypeCode,
    String documentNumber,
    String gender,
    LocalDate birthDate,
    LocalDate validFrom,
    LocalDate validUntil
) {
}
