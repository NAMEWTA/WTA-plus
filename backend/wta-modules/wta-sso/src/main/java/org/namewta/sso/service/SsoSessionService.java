package org.namewta.sso.service;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.api.SsoSessionSnapshot;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.sso.port.SsoSessionPort;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import java.time.Instant;

/**
 * SSO 域会话：只认本仓密码，不签发业务 Token。
 */
@Service
public class SsoSessionService {

    private final SsoIdentityPort identityPort;
    private final SsoSessionPort sessionPort;
    private final Clock clock;

    /**
     * 组装会话服务。
     *
     * @param identityPort 认人
     * @param sessionPort  会话存储
     */
    @Autowired
    public SsoSessionService(SsoIdentityPort identityPort, SsoSessionPort sessionPort) {
        this(identityPort, sessionPort, Clock.systemUTC());
    }

    /** 注入可控时钟，用于验证原始认证时间与固定过期边界。 */
    SsoSessionService(SsoIdentityPort identityPort, SsoSessionPort sessionPort, Clock clock) {
        this.identityPort = identityPort;
        this.sessionPort = sessionPort;
        this.clock = clock;
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
     * 读取供身份协议使用的原始认证快照；旧会话继续供第一方读取，但须重新认证才可用于 OIDC。
     * @param sessionId 中央会话标识
     * @return 原始认证快照；时间缺失、异常或过期时返回 null
     */
    public SsoSessionSnapshot currentSnapshot(String sessionId) {
        SsoAuthenticatedUser user = sessionPort.find(sessionId);
        Instant now = clock.instant();
        if (user == null || user.getUserId() == null || user.getUserId() <= 0
            || user.getAuthenticatedAt() == null || user.getExpiresAt() == null
            || user.getAuthenticatedAt().isAfter(now) || !user.getExpiresAt().isAfter(now)
            || !user.getExpiresAt().isAfter(user.getAuthenticatedAt())) {
            return null;
        }
        return new SsoSessionSnapshot(user.getUserId(), user.getUsername(),
            user.getAuthenticatedAt(), user.getExpiresAt());
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
