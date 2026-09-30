package org.namewta.profile.person.domain.model.read;

import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

/** 当前有效个人档案的字段白名单查询读模型，不得直接序列化为外部响应。 */
@Data
public class PersonDisclosureRow {
    private Long userId;
    private Long profileId;
    private Instant verifiedAt;
    private String fullName;
    private String gender;
    private LocalDate birthDate;
    private String documentType;
    private String documentNumberMasked;
    private String documentNumber;
    private LocalDate validFrom;
    private LocalDate validUntil;
}

