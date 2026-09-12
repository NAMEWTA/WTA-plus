package org.namewta.profile.enterprise.domain.application;

/** EnterpriseDraftUpdate 应用层领域模型。 */
public record EnterpriseDraftUpdate(EnterpriseIdentityFields fields, Long targetProfileId, int expectedVersion) {
}
