package org.namewta.oidc.usecase;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.service.OidcAuthorizationPersistenceService;
import org.springframework.stereotype.Component;

/** 定时维护仅编排有界的过期授权删除。 */
@Component
@RequiredArgsConstructor
public class OidcMaintenanceUseCase {
    private final OidcAuthorizationPersistenceService authorizations;

    /** 每轮最多删除五百条，后续批次由下个调度周期处理。 */
    public int purgeExpired() {
        return authorizations.purgeExpired();
    }
}
