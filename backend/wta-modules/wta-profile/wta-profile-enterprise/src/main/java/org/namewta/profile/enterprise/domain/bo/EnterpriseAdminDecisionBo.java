package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** EnterpriseAdminDecisionBo 请求参数模型。 */
public record EnterpriseAdminDecisionBo(@NotBlank @Size(max = 32) String decision,
                                        @NotBlank @Size(max = 500) String reason) {
}
