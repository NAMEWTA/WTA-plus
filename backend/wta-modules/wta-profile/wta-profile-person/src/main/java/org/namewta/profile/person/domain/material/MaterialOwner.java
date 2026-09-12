package org.namewta.profile.person.domain.material;

import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;

/** MaterialOwner 材料领域模型。 */
public record MaterialOwner(MaterialOwnerKey key, Long applicantUserId) {
}
