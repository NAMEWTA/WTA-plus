package org.namewta.profile.person.domain.application;

import java.time.Instant;

/** PersonActiveProjection 应用层领域模型。 */
public record PersonActiveProjection(long userId, long personProfileId, Instant verifiedAt) {
}
