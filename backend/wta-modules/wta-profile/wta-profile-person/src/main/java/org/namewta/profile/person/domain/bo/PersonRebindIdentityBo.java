package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** PersonRebindIdentityBo 请求参数模型。 */
public record PersonRebindIdentityBo(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Size(max = 64) String documentTypeCode,
    @NotBlank @Size(max = 128) String documentNumber,
    @NotBlank @Size(max = 16) String gender,
    @NotNull LocalDate birthDate,
    LocalDate validFrom,
    LocalDate validUntil
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
