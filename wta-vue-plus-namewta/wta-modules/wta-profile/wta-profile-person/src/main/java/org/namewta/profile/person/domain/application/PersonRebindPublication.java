package org.namewta.profile.person.domain.application;

import org.namewta.profile.person.event.PersonReboundEvent;

/** PersonRebindPublication 应用层领域模型。 */
public record PersonRebindPublication(long personSubmissionId, long personVersionId, PersonReboundEvent event) {
}
