package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.Positive;

/** EnterpriseAdminMaterialBo 请求参数模型。 */
public record EnterpriseAdminMaterialBo(@Positive Long ossId, @Positive Long materialNodeId) {
}
