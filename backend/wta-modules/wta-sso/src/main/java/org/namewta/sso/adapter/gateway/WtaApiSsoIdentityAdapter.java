package org.namewta.sso.adapter.gateway;

import lombok.RequiredArgsConstructor;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.api.SsoIdentityService;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.system.api.model.LoginUser;
import org.springframework.stereotype.Component;

/**
 * 经 wta-api 认人并组装业务登录态。
 */
@Component
@RequiredArgsConstructor
public class WtaApiSsoIdentityAdapter implements SsoIdentityPort {

    private final SsoIdentityService identityService;

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoAuthenticatedUser verifyPassword(String username, String password) {
        return identityService.verifyPassword(username, password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertClientAccess(Long userId, String clientId) {
        identityService.assertClientAccess(userId, clientId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginUser buildLoginUser(Long userId, String clientId) {
        return identityService.buildLoginUser(userId, clientId);
    }
}
