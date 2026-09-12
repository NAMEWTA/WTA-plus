package org.namewta.profile.person.domain.model.read;

import lombok.Data;

import java.time.LocalDate;

/** 个人档案查询读模型。 */
@Data
public class PersonProfileRow {

    private Long personProfileId;
    private Long previousProfileId;
    private Long currentVersionId;
    private String fullName;
    private String documentTypeCode;
    private String documentNumber;
    private String identityKey;
    private String gender;
    private LocalDate birthDate;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String status;
    private Integer version;
}
