package org.namewta.oidc.domain.vo;

import java.time.LocalDateTime;

/** 管理页密钥目录，仅显示版本、用途与活动状态。 */
public record OidcKeyVo(String keyId, String kind, boolean active, LocalDateTime createdAt) {}
