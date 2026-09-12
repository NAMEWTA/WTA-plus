package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.PositiveOrZero;

/** PersonApplicationSubmitBo 请求参数模型。 */
public record PersonApplicationSubmitBo(@PositiveOrZero int expectedVersion) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
