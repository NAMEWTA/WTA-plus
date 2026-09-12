package org.namewta.profile.enterprise.domain.bo;

/** EnterpriseAdminQueryBo 请求参数模型。 */
public record EnterpriseAdminQueryBo(
    String enterpriseName,
    String unifiedCreditCode,
    String status,
    int pageNum,
    int pageSize
) {
}
