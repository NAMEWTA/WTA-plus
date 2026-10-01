package org.namewta.sso.adapter.api;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.api.SsoSessionAccess;
import org.namewta.sso.api.SsoSessionSnapshot;
import org.namewta.sso.usecase.SsoSessionUseCase;
import org.springframework.stereotype.Component;

/** 经会话用例提供跨模块会话访问，不使其他模块直接依赖 Redis 或 SSO 实现。 */
@Component
@RequiredArgsConstructor
public class SsoSessionAccessAdapter implements SsoSessionAccess {
    private final SsoSessionUseCase useCase;

    @Override
    public SsoSessionSnapshot current(String sessionId) {
        return useCase.currentSnapshot(sessionId);
    }

    @Override
    public void logout(String sessionId) {
        useCase.logout(sessionId);
    }
}
