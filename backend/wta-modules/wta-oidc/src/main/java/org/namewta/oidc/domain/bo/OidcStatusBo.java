package org.namewta.oidc.domain.bo;

import jakarta.validation.constraints.NotNull;

/** 应用启用状态变更输入。 */
public record OidcStatusBo(@NotNull Integer version, @NotNull Boolean enabled) {}
