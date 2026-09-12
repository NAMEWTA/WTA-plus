package org.namewta.profile.person.domain.model.read;

import java.time.Instant;

/** 材料引用关系查询读模型。 */
public record MaterialReferenceRow(Long materialRefId, String ownerType, Long ownerId, String profileType,
                                   Long ossId, Long materialNodeId, String materialTagCode,
                                   String materialTagName, String fileName, Long fileSize,
                                   String fileExtension, String mimeType, String status, String immutableFlag,
                                   Instant attachedTime, Instant detachedTime, Integer version) {
}
