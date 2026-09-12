package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** PersonAdminBindingBo 请求参数模型。 */
public record PersonAdminBindingBo(@NotBlank @Size(max = 32) String action,
                                   @NotBlank @Size(max = 500) String reason,
                                   @PositiveOrZero int expectedBindingVersion) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
