package org.namewta.profile.person.domain.application;

/** PersonDraftUpdate 应用层领域模型。 */
public record PersonDraftUpdate(PersonIdentityFields fields, Long targetProfileId, int expectedVersion) {
}
