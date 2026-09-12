package org.namewta.profile.enterprise.domain.vo;

import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;

import java.util.List;

/** EnterpriseProfileDetailVo 对外返回模型。 */
public record EnterpriseProfileDetailVo(
    EnterpriseProfileSummaryVo profile,
    List<EnterpriseProfileVersionVo> versions,
    List<EnterpriseProfileBindingVo> bindings,
    List<EnterpriseProfileSourceVo> sources,
    List<EnterpriseProfileAuditVo> audits,
    List<MaterialReferenceView> currentMaterials
) {
}
