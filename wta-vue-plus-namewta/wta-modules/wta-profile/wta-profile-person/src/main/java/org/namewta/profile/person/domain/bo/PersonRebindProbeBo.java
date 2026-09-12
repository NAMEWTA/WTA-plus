package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PersonRebindProbeBo 请求参数模型。 */
public record PersonRebindProbeBo(@NotBlank @Size(max = 64) String documentTypeCode,
                                  @NotBlank @Size(max = 128) String documentNumber) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
