package org.namewta.sso.adapter.store;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.sso.api.SsoAuthenticatedUser;
import org.namewta.sso.config.SsoProperties;
import org.namewta.sso.port.SsoSessionPort;
import org.springframework.stereotype.Component;

/**
 * SSO 域会话存放在 Redis，Cookie 只持有会话标识。
 */
@Component
@RequiredArgsConstructor
public class RedisSsoSessionStore implements SsoSessionPort {

    private static final String KEY_PREFIX = "sso:session:";

    private final SsoProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String create(SsoAuthenticatedUser user) {
        String sessionId = Long.toUnsignedString(IdGeneratorUtil.nextLongId(), 16)
            + Long.toUnsignedString(IdGeneratorUtil.nextLongId(), 16);
        RedisUtils.setCacheObject(KEY_PREFIX + sessionId, user, properties.getSessionTtl());
        return sessionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoAuthenticatedUser find(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }
        return RedisUtils.getCacheObject(KEY_PREFIX + sessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return;
        }
        RedisUtils.deleteObject(KEY_PREFIX + sessionId);
    }
}
