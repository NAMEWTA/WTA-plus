package org.namewta.profile.person.domain.vo;

import java.time.Instant;
import java.time.LocalDate;

/** PersonProfileVersionVo 对外返回模型。 */
public record PersonProfileVersionVo(
    long versionId,
    int versionNo,
    String sourceType,
    long sourceId,
    String fullName,
    String documentTypeCode,
    String documentNumber,
    String gender,
    LocalDate birthDate,
    LocalDate validFrom,
    LocalDate validUntil,
    String status,
    Instant publishedTime
) {
}
