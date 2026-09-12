package org.namewta.system.temporarypassword;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis 的用户级临时密码存储。
 */
@Component
@RequiredArgsConstructor
public class RedisTemporaryPasswordStore implements TemporaryPasswordStore {

    private static final String KEY_PREFIX = "auth:temporary-password:user:";
    private static final String COMPARE_AND_DELETE = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        end
        return 0
        """;

    private final RedissonClient redissonClient;

    @Override
    public void store(Long userId, String passwordHash, Duration timeToLive) {
        redissonClient.<String>getBucket(key(userId), StringCodec.INSTANCE).set(passwordHash, timeToLive);
    }

    @Override
    public String read(Long userId) {
        return redissonClient.<String>getBucket(key(userId), StringCodec.INSTANCE).get();
    }

    @Override
    public boolean compareAndDelete(Long userId, String expectedHash) {
        Number deleted = redissonClient.getScript(StringCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            COMPARE_AND_DELETE,
            RScript.ReturnType.LONG,
            List.of(key(userId)),
            expectedHash);
        return deleted != null && deleted.longValue() == 1;
    }

    /**
     * 返回用户临时密码 Redis key；公开仅用于运行时接缝验证与运维定位。
     */
    public static String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
