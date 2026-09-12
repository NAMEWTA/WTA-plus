package org.namewta.common.openapi.ratelimit;

import org.namewta.common.openapi.gateway.OpenApiStateStoreException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Redisson-backed atomic limiter with scope material hashed out of Redis key names.
 */
public final class RedissonOpenApiRateLimiter implements OpenApiRateLimiter {

    private static final String KEY_PREFIX = "openapi:rate:";

    private final RedissonClient redissonClient;

    public RedissonOpenApiRateLimiter(RedissonClient redissonClient) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
    }

    @Override
    public boolean acquire(String scope, int limit, Duration interval) {
        Objects.requireNonNull(scope, "scope");
        requirePositive(limit, interval);
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter(key(scope, limit, interval));
            limiter.trySetRate(RateType.OVERALL, limit, interval);
            return limiter.tryAcquire();
        } catch (RuntimeException exception) {
            throw new OpenApiStateStoreException(exception);
        }
    }

    private static String key(String scope, int limit, Duration interval) {
        try {
            String versionedScope = scope + '\n' + limit + '\n' + interval.toMillis();
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(versionedScope.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void requirePositive(int limit, Duration interval) {
        Objects.requireNonNull(interval, "interval");
        if (limit <= 0 || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("rate limit and interval must be positive");
        }
    }
}
