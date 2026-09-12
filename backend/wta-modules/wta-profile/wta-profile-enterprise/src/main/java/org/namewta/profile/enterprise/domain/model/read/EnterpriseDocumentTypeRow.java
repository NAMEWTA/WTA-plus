package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

/** 企业证件类型规则查询读模型。 */
@Data
public class EnterpriseDocumentTypeRow {

    private String documentTypeCode;
    private String numberPattern;
    private String validityRequired;
}
