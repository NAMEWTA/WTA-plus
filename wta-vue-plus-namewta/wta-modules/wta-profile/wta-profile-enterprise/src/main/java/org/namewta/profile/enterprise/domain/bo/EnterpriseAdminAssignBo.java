package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** EnterpriseAdminAssignBo 请求参数模型。 */
public record EnterpriseAdminAssignBo(@Positive Long userId,
                                      @NotBlank @Size(max = 500) String reason) {
}
