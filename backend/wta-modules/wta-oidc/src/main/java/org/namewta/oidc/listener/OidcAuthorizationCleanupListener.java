package org.namewta.oidc.listener;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.usecase.OidcMaintenanceUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每分钟清除一小批超出保留期的加密授权状态。 */
@Component
@RequiredArgsConstructor
public class OidcAuthorizationCleanupListener {
    private final OidcMaintenanceUseCase maintenance;

    /** 多实例可重复执行，由主库有界删除保证一致性。 */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void cleanup() {
        maintenance.purgeExpired();
    }
}
