package org.namewta.sso.adapter.store;

import lombok.RequiredArgsConstructor;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.port.SsoSessionPort;
import org.namewta.sso.support.SsoBearerTokens;
import org.springframework.stereotype.Component;

/**
 * SSO 域会话存放在 Redis，Cookie 只持有会话标识。
 */
@Component
@RequiredArgsConstructor
public class RedisSsoSessionStore implements SsoSessionPort {

    // 硬切换使旧雪花ID会话失效；旧键自然到期，不执行批量删除。
    private static final String KEY_PREFIX = "sso:session:v2:";

    private final SsoProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String create(SsoAuthenticatedUser user) {
        String sessionId = SsoBearerTokens.create();
        RedisUtils.setCacheObject(KEY_PREFIX + sessionId, user, properties.getSessionTtl());
        return sessionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoAuthenticatedUser find(String sessionId) {
        if (!SsoBearerTokens.isValid(sessionId)) {
            return null;
        }
        return RedisUtils.getCacheObject(KEY_PREFIX + sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String sessionId) {
        if (!SsoBearerTokens.isValid(sessionId)) {
            return;
        }
        RedisUtils.deleteObject(KEY_PREFIX + sessionId);
    }
}
