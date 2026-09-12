package org.namewta.profile.person.domain.vo;

import java.time.Instant;
import java.time.LocalDate;

/** PersonProfileSummaryVo 对外返回模型。 */
public record PersonProfileSummaryVo(
    long profileId,
    Long previousProfileId,
    String fullName,
    String documentTypeCode,
    String documentNumber,
    String gender,
    LocalDate birthDate,
    String status,
    Long bindingUserId,
    String bindingStatus,
    Instant createTime
) {
}
