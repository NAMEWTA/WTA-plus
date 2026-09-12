package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

/** 企业认证申请查询读模型。 */
@Data
public class EnterpriseVerificationApplicationRow {

    private Long applicationId;
    private Long submissionId;
    private String providerCode;
    private String status;
}
