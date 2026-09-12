package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** PersonAdminRevokeBo 请求参数模型。 */
public record PersonAdminRevokeBo(@NotBlank @Size(max = 500) String reason,
                                  @PositiveOrZero int expectedVersion) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
