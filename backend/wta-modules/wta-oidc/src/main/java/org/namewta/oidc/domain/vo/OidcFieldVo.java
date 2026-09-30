package org.namewta.oidc.domain.vo;

/** 管理端可披露字段目录，不含实际用户数据。 */
public record OidcFieldVo(
        String key,
        String label,
        String scope,
        boolean sensitive,
        boolean defaultEnabled,
        String group) {}
