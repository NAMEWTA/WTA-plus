package org.namewta.oidc.domain.bo;

import jakarta.validation.constraints.NotNull;

import org.namewta.oidc.domain.OidcServiceSettings;

/** 管理页必须携带当前乐观版本，避免覆盖其他管理员修改。 */
public record OidcServiceBo(@NotNull Integer version, @NotNull OidcServiceSettings settings) {}
