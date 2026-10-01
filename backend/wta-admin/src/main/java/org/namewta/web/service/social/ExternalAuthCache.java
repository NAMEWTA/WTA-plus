package org.namewta.web.service.social;

import org.namewta.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 本地认证事务的 Redis 边界，原子消费不依赖进程内锁或临时内存回退。 */
@Component
public class ExternalAuthCache {
    public String read(String key) {
        return RedisUtils.getCacheObject(key);
    }

    public void put(String key, String encrypted, Duration ttl) {
        RedisUtils.setCacheObject(key, encrypted, ttl);
    }

    public boolean consume(String key, String expected) {
        return RedisUtils.deleteObjectIfEquals(key, expected);
    }
}
