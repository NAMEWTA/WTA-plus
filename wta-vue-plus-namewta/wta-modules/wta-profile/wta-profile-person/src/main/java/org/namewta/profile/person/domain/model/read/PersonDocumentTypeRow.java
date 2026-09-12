package org.namewta.profile.person.domain.model.read;

import lombok.Data;

/** 个人证件类型规则查询读模型。 */
@Data
public class PersonDocumentTypeRow {

    private String documentTypeCode;
    private String numberPattern;
    private String validityRequired;
}
