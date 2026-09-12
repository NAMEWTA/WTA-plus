package org.namewta.profile.person.domain.model.read;

import lombok.Data;

/** 个人认证申请查询读模型。 */
@Data
public class PersonVerificationApplicationRow {

    private Long applicationId;
    private Long submissionId;
    private String providerCode;
    private String status;
}
