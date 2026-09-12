package org.namewta.profile.enterprise.domain.vo;

import java.time.Instant;

/** EnterpriseProfileSummaryVo 对外返回模型。 */
public record EnterpriseProfileSummaryVo(
    long profileId,
    Long previousProfileId,
    String enterpriseName,
    String unifiedCreditCode,
    String enterpriseType,
    String legalRepresentativeName,
    String status,
    Long bindingUserId,
    String bindingStatus,
    Instant createTime
) {
}
