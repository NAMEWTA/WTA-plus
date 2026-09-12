package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.LocalDate;

/** 个人换绑候选读模型，用于保存精确身份匹配结果。 */
@Data
public class PersonRebindCandidateRow {
    private Long personProfileId;
    private String fullName;
    private String documentTypeCode;
    private String documentNumber;
    private String identityKey;
    private String gender;
    private LocalDate birthDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Long personBindingId;
    private Long oldUserId;
    private Integer bindingVersion;
}
