package org.namewta.profile.person.domain.material;

import org.namewta.profile.api.material.ProfileMaterialPort.MaterialNodeType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialScope;

/** MaterialNode 材料领域模型。 */
public record MaterialNode(Long materialNodeId, Long parentId, MaterialNodeType nodeType, int nodeDepth,
                           MaterialScope scope, String materialTagCode, String nodeName,
                           boolean systemRequired, boolean enabled, int orderNum, int version) {
}
