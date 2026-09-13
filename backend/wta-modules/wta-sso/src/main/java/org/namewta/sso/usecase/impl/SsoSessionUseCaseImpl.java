package org.namewta.sso.usecase.impl;

import lombok.RequiredArgsConstructor;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.service.SsoSessionService;
import org.namewta.sso.usecase.SsoSessionUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * SSO 会话用例编排。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "namewta.sso", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SsoSessionUseCaseImpl implements SsoSessionUseCase {

    private final SsoSessionService sessionService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String login(String username, String password) {
        return sessionService.login(username, password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoAuthenticatedUser current(String sessionId) {
        return sessionService.current(sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout(String sessionId) {
        sessionService.logout(sessionId);
    }
}
