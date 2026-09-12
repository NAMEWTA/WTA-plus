package org.namewta.common.openapi.nonce;

import org.namewta.common.openapi.gateway.OpenApiStateStoreException;
import org.redisson.api.RedissonClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Redis SET-if-absent nonce store. AppKey and nonce are hashed out of Redis key names.
 */
public final class RedissonOpenApiNonceStore implements OpenApiNonceStore {

    private static final String KEY_PREFIX = "openapi:nonce:";

    private final RedissonClient redissonClient;

    public RedissonOpenApiNonceStore(RedissonClient redissonClient) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
    }

    @Override
    public boolean register(String appKey, String nonce, Duration ttl) {
        requirePositive(ttl);
        try {
            return redissonClient.<String>getBucket(key(appKey, nonce)).setIfAbsent("1", ttl);
        } catch (RuntimeException exception) {
            throw new OpenApiStateStoreException(exception);
        }
    }

    private static String key(String appKey, String nonce) {
        Objects.requireNonNull(appKey, "appKey");
        Objects.requireNonNull(nonce, "nonce");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest((appKey + '\n' + nonce).getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void requirePositive(Duration value) {
        Objects.requireNonNull(value, "ttl");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }
}
