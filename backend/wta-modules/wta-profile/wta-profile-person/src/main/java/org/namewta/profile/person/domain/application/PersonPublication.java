package org.namewta.profile.person.domain.application;

/** PersonPublication 应用层领域模型。 */
public record PersonPublication(
    long personProfileId,
    long personVersionId,
    long personBindingId,
    boolean successorProfile
) {
}
