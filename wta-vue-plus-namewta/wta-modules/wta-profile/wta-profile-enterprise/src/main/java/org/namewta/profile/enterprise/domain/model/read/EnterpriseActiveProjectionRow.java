package org.namewta.profile.enterprise.domain.model.read;

import lombok.Data;

import java.time.Instant;

/** 用户当前企业档案的查询读模型。 */
@Data
public class EnterpriseActiveProjectionRow {

    private Long userId;
    private Long enterpriseProfileId;
    private Instant verifiedAt;
}
