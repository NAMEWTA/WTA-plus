package org.namewta.oidc.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.port.OidcLogoutDeliveryPort;
import org.namewta.oidc.service.OidcApplicationService;
import org.namewta.oidc.service.OidcAuthorizationPersistenceService;
import org.namewta.oidc.service.OidcLogoutService;
import org.springframework.stereotype.Service;

/** 中央全退的持久协议参与者与异步投递用例。 */
@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class OidcLogoutUseCase {
    private final OidcAuthorizationPersistenceService authorizations;
    private final OidcApplicationService applications;
    private final OidcLogoutService logout;
    private final OidcProperties properties;
    private final OidcLogoutDeliveryPort delivery;

    /** 被SSO在同一主库事务中调用，不能在此发HTTP请求。 */
    @DSTransactional
    public void revoke(String hash) {
        for (var grant : authorizations.sessionGrants(hash))
            logout.enqueue(
                    properties.getIssuer(),
                    grant,
                    applications.findForLogout(grant.getApplicationId()));
        authorizations.revokeSession(hash);
    }

    /** 每轮有界投递，一个任务一个租约，外呼最多10秒。 */
    public void dispatch() {
        for (int i = 0; i < 10; i++) {
            var row = logout.claim();
            if (row == null) return;
            try {
                logout.result(row, delivery.deliver(row));
            } catch (RuntimeException e) {
                log.warn("OIDC退出等待重试 id={}", row.getLogoutOutboxId());
                logout.result(row, 0);
            }
            if (Thread.currentThread().isInterrupted()) return;
        }
    }
}
