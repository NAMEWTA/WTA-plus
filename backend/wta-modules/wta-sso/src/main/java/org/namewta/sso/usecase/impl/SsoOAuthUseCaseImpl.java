package org.namewta.sso.usecase.impl;

import lombok.RequiredArgsConstructor;
import org.namewta.sso.domain.SsoOAuthCommands;
import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.sso.service.SsoAuthorizationService;
import org.namewta.sso.usecase.SsoOAuthUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 授权码用例编排。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "namewta.sso", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SsoOAuthUseCaseImpl implements SsoOAuthUseCase {

    private final SsoAuthorizationService authorizationService;

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoOAuthCommands.AuthorizeResult authorize(SsoOAuthCommands.AuthorizeCommand command) {
        return authorizationService.authorize(command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoBusinessTokenPort.IssuedToken exchange(SsoOAuthCommands.TokenCommand command) {
        return authorizationService.exchange(command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revoke(String token) {
        authorizationService.revoke(token);
    }
}
