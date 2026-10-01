package org.namewta.sso.adapter.api;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.api.SsoSessionLifecycle;
import org.namewta.sso.usecase.SsoSessionLifecycleUseCase;
import org.springframework.stereotype.Component;

/** OIDC等内部签发者通过公共接口加入会话串行边界。 */
@Component
@RequiredArgsConstructor
public class SsoSessionLifecycleAdapter implements SsoSessionLifecycle {
    private final SsoSessionLifecycleUseCase useCase;

    @Override
    public boolean hasActiveSessions() {
        return useCase.hasActiveSessions();
    }

    @Override
    public void revokeAllSessions() {
        useCase.revokeAllSessions();
    }

    @Override
    public void requireActiveLocked(String sid) {
        useCase.requireActiveLocked(sid);
    }
}
