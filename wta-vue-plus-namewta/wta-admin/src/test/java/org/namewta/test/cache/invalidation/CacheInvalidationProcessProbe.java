package org.namewta.test.cache.invalidation;

import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.namewta.common.redis.cache.RedissonCacheInvalidationTransport;
import org.namewta.common.redis.manager.CaffeineCacheDecorator;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.spring.cache.RedissonCache;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Child-process probe used by the two-JVM Redis integration test.
 */
public final class CacheInvalidationProcessProbe {

    private static final String KEY = "satoken:probe:secret-token-value";

    private CacheInvalidationProcessProbe() {
    }

    public static void main(String[] args) throws Exception {
        RedissonClient client = client(Integer.parseInt(args[0]));
        try (ClusterCacheInvalidationCoordinator coordinator = new ClusterCacheInvalidationCoordinator(
            new RedissonCacheInvalidationTransport(client, JsonMapper.builder().build()),
            Duration.ofSeconds(2), args[1]);
             BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            RedissonCache distributedCache = new RedissonCache(client.getMap("probe:spring-cache"), true);
            distributedCache.putIfAbsent(KEY, "cached");
            CaffeineCacheDecorator cache = new CaffeineCacheDecorator(
                "probe:spring-cache#60s", distributedCache,
                com.github.benmanes.caffeine.cache.Caffeine.newBuilder().build(), coordinator);
            cache.get(KEY);
            System.out.println("PROBE_READY");
            String command;
            while ((command = input.readLine()) != null) {
                if ("invalidate".equals(command)) {
                    cache.evict(KEY);
                    System.out.println("PROBE_INVALIDATED");
                } else if ("contains".equals(command)) {
                    System.out.println(cache.get(KEY) == null ? "PROBE_MISSING" : "PROBE_PRESENT");
                } else if ("close".equals(command)) {
                    break;
                }
            }
        } finally {
            client.shutdown();
        }
    }

    private static RedissonClient client(int port) {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        return Redisson.create(config);
    }
}
