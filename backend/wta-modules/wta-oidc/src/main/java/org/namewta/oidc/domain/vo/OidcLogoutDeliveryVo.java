package org.namewta.oidc.domain.vo;

import java.time.LocalDateTime;

/** 管理投递状态，不暴露subject、sid或退出凭据。 */
public record OidcLogoutDeliveryVo(
        String logoutOutboxId,
        String clientId,
        String status,
        Integer attempts,
        LocalDateTime nextAttemptAt,
        String lastError,
        LocalDateTime createTime) {}
