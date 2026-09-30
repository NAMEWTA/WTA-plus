package org.namewta.oidc.adapter.store;

import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.oidc.port.OidcInteractionStore;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 事务上下文有界 TTL 与原子消费。 */
@Component
public class RedisOidcInteractionStore implements OidcInteractionStore {
    /** 以明确有效期写入短期密文上下文。 */
    public void put(String key, String value, long seconds) {
        RedisUtils.setCacheObject(key, value, Duration.ofSeconds(seconds));
    }

    /** 读取有界存储中的短期密文上下文。 */
    public String get(String key) {
        return RedisUtils.getCacheObject(key);
    }

    /** 删除短期上下文或撤销指定授权，不影响其他授权。 */
    public boolean remove(String key, String expected) {
        return RedisUtils.deleteObjectIfEquals(key, expected);
    }
}
