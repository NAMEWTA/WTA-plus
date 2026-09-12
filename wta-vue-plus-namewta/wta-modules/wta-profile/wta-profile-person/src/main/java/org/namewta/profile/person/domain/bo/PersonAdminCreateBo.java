package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** PersonAdminCreateBo 请求参数模型。 */
public record PersonAdminCreateBo(
    @Valid @NotNull PersonAdminIdentityBo identity,
    @Positive Long bindUserId,
    @NotBlank @Size(max = 500) String reason,
    @Valid @Size(max = 10) List<PersonAdminMaterialBo> materials
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
