package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.PositiveOrZero;

/** PersonRebindSubmitBo 请求参数模型。 */
public record PersonRebindSubmitBo(@PositiveOrZero int expectedVersion) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
