package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** EnterpriseAdminBindingBo 请求参数模型。 */
public record EnterpriseAdminBindingBo(@NotBlank @Size(max = 32) String action,
                                       @NotBlank @Size(max = 500) String reason,
                                       @PositiveOrZero int expectedBindingVersion) {
}
