package org.namewta.profile.person.event;

/** PersonReboundEvent 领域事件模型。 */
public record PersonReboundEvent(
    long personProfileId,
    long personApplicationId,
    long oldUserId
) {
}
