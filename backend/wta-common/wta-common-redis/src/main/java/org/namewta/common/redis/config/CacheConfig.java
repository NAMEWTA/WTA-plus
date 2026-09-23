package org.namewta.common.redis.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.namewta.common.redis.cache.CacheInvalidationTransport;
import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.namewta.common.redis.cache.RedissonCacheInvalidationTransport;
import org.namewta.common.redis.manager.PlusSpringCacheManager;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 */
@AutoConfiguration
@EnableCaching
public class CacheConfig {

    /**
     * caffeine 本地缓存处理器
     */
    @Bean
    public Cache<Object, Object> caffeine() {
        return Caffeine.newBuilder()
            // 设置最后一次写入或访问后经过固定时间过期
            .expireAfterWrite(30, TimeUnit.SECONDS)
            // 初始的缓存空间大小
            .initialCapacity(100)
            // 缓存的最大条数
            .maximumSize(1000)
            .build();
    }

    /**
     * Creates the Redis transport for cluster invalidation.
     */
    @Bean
    public CacheInvalidationTransport cacheInvalidationTransport(RedissonClient redissonClient,
                                                                  JsonMapper jsonMapper) {
        return new RedissonCacheInvalidationTransport(redissonClient, jsonMapper);
    }

    /**
     * Coordinates acknowledged invalidation across application nodes.
     */
    @Bean(destroyMethod = "close")
    public ClusterCacheInvalidationCoordinator clusterCacheInvalidationCoordinator(
        CacheInvalidationTransport transport) {
        return new ClusterCacheInvalidationCoordinator(transport);
    }

    /**
     * Custom Spring cache manager with local and distributed cache layers.
     */
    @Bean
    public CacheManager cacheManager(Cache<Object, Object> caffeine,
                                     ClusterCacheInvalidationCoordinator invalidationCoordinator) {
        return new PlusSpringCacheManager(caffeine, invalidationCoordinator);
    }

}
