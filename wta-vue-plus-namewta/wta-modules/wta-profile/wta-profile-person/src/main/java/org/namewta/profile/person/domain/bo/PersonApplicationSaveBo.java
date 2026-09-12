package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** PersonApplicationSaveBo 请求参数模型。 */
public record PersonApplicationSaveBo(
    @Size(max = 100) String fullName,
    @Size(max = 64) String documentTypeCode,
    @Size(max = 128) String documentNumber,
    @Size(max = 16) String gender,
    LocalDate birthDate,
    LocalDate validFrom,
    LocalDate validUntil,
    @PositiveOrZero int expectedVersion
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
