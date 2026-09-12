package org.namewta.profile.enterprise.domain.application;

import java.time.Instant;

/** EnterpriseActiveProjection 应用层领域模型。 */
public record EnterpriseActiveProjection(long userId, long enterpriseProfileId, Instant verifiedAt) {
}
