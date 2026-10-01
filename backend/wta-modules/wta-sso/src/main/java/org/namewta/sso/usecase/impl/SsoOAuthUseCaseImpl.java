package org.namewta.sso.usecase.impl;

import org.namewta.sso.domain.SsoOAuthCommands;
import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.sso.service.SsoAuthorizationService;
import org.namewta.sso.usecase.SsoOAuthUseCase;
import org.springframework.stereotype.Service;

/** 授权码用例编排。 */
@Service
public class SsoOAuthUseCaseImpl implements SsoOAuthUseCase {

    private final SsoAuthorizationService authorizationService;
    private final org.namewta.sso.service.SsoPersistentSessionService sessions;

    /** 生产换票统一持有中央会话锁并登记关联票。 */
    @org.springframework.beans.factory.annotation.Autowired
    public SsoOAuthUseCaseImpl(
            SsoAuthorizationService service,
            org.namewta.sso.service.SsoPersistentSessionService sessions) {
        this.authorizationService = service;
        this.sessions = sessions;
    }

    /** 保留旧显式装配，仅生产完整构造器提供持久退出关联。 */
    public SsoOAuthUseCaseImpl(SsoAuthorizationService service) {
        this(service, null);
    }

    /** {@inheritDoc} */
    @Override
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public SsoOAuthCommands.AuthorizeResult authorize(SsoOAuthCommands.AuthorizeCommand command) {
        if (sessions != null && command.user() != null) sessions.requireLocked(command.sessionId());
        return authorizationService.authorize(command);
    }

    /** {@inheritDoc} */
    @Override
    @com.baomidou.dynamic.datasource.annotation.DSTransactional
    public SsoBusinessTokenPort.IssuedToken exchange(SsoOAuthCommands.TokenCommand command) {
        String hash = sessions == null ? null : authorizationService.sessionHash(command.code());
        if (sessions != null) sessions.requireHashLocked(hash);
        var issued = authorizationService.exchange(command);
        if (sessions != null) {
            try {
                sessions.register(hash, issued.accessToken(), issued.clientId());
            } catch (RuntimeException failure) {
                authorizationService.revoke(issued.accessToken());
                throw failure;
            }
        }
        return issued;
    }

    /** {@inheritDoc} */
    @Override
    public void revoke(String token) {
        authorizationService.revoke(token);
    }
}
