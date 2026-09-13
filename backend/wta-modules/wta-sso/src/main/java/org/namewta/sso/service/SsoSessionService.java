package org.namewta.sso.service;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.sso.port.SsoSessionPort;
import org.springframework.stereotype.Service;

/**
 * SSO 域会话：只认本仓密码，不签发业务 Token。
 */
@Service
public class SsoSessionService {

    private final SsoIdentityPort identityPort;
    private final SsoSessionPort sessionPort;

    /**
     * 组装会话服务。
     *
     * @param identityPort 认人
     * @param sessionPort  会话存储
     */
    public SsoSessionService(SsoIdentityPort identityPort, SsoSessionPort sessionPort) {
        this.identityPort = identityPort;
        this.sessionPort = sessionPort;
    }

    /**
     * 密码登录并建立 SSO 会话。
     *
     * @param username 用户名
     * @param password 密码
     * @return 会话标识
     */
    public String login(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new ServiceException("用户名或密码错误");
        }
        SsoAuthenticatedUser user = identityPort.verifyPassword(username, password);
        return sessionPort.create(user);
    }

    /**
     * 读取会话。
     *
     * @param sessionId 会话标识
     * @return 用户
     */
    public SsoAuthenticatedUser current(String sessionId) {
        return sessionPort.find(sessionId);
    }

    /**
     * 注销 SSO 会话。
     *
     * @param sessionId 会话标识
     */
    public void logout(String sessionId) {
        sessionPort.delete(sessionId);
    }
}
