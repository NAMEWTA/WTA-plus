package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** EnterpriseApplicationProbeBo 请求参数模型。 */
public record EnterpriseApplicationProbeBo(@NotBlank @Size(max = 64) String unifiedCreditCode) {
}
