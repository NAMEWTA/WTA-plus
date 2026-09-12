package org.namewta.profile.person.domain.bo;

import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** PersonRebindMatchBo 请求参数模型。 */
public record PersonRebindMatchBo(@Valid @NotNull PersonRebindIdentityBo identity) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
