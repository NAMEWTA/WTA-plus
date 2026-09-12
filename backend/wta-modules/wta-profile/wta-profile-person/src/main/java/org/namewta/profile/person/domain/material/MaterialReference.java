package org.namewta.profile.person.domain.material;

import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;

import java.time.Instant;

/** MaterialReference 材料领域模型。 */
public record MaterialReference(Long materialRefId, MaterialOwnerKey owner, Long ossId, Long materialNodeId,
                                String materialTagCode, String materialTagName, String fileName, long fileSize,
                                String fileExtension, String mimeType, boolean attached, boolean immutableEvidence,
                                Instant attachedTime, Instant detachedTime, int version) {
}
