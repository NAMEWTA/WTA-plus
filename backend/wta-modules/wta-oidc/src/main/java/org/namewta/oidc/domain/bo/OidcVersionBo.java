package org.namewta.oidc.domain.bo;

import jakarta.validation.constraints.NotNull;

/** 防止陈旧管理写覆盖。 */
public record OidcVersionBo(@NotNull Integer version) {}
