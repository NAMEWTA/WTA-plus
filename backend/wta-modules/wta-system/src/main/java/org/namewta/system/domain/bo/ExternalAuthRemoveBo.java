package org.namewta.system.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** 以版本进行并发校验的单条删除请求。 */
public record ExternalAuthRemoveBo(@NotNull @Positive Long id, @NotNull @PositiveOrZero Long version) {
}
