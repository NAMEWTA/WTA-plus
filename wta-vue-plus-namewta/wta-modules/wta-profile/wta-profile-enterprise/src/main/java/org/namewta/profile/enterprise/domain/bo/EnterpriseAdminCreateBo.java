package org.namewta.profile.enterprise.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** EnterpriseAdminCreateBo 请求参数模型。 */
public record EnterpriseAdminCreateBo(
    @Valid @NotNull EnterpriseAdminIdentityBo identity,
    @Positive Long bindUserId,
    @NotBlank @Size(max = 500) String reason,
    @Size(max = 10) List<@Valid EnterpriseAdminMaterialBo> materials
) {
}
