package org.namewta.profile.enterprise.domain.application;

/** EnterprisePublication 应用层领域模型。 */
public record EnterprisePublication(
    long enterpriseProfileId,
    long enterpriseVersionId,
    long enterpriseBindingId,
    boolean successorProfile
) {
}
