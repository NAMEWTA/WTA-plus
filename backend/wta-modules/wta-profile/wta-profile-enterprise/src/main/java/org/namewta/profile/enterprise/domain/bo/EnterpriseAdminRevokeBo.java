package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** EnterpriseAdminRevokeBo 请求参数模型。 */
public record EnterpriseAdminRevokeBo(@NotBlank @Size(max = 500) String reason,
                                      @PositiveOrZero int expectedVersion) {
}
