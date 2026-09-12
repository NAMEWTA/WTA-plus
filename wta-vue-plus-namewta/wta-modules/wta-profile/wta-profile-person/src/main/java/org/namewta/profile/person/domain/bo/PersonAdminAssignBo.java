package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** PersonAdminAssignBo 请求参数模型。 */
public record PersonAdminAssignBo(@Positive Long userId, @NotBlank @Size(max = 500) String reason)
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
