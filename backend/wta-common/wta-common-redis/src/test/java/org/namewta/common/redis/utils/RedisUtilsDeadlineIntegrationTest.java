package org.namewta.common.redis.utils;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.redis.config.RedisConfig;
import org.namewta.common.redis.config.properties.RedissonProperties;
import org.redisson.Redisson;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 隔离 Redis 的绝对截止与生产 codec/NameMapper 兼容验收，须在独立测试 JVM 执行。 */
@Tag("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "notify.deadline.redis.integration", matches = "true")
class RedisUtilsDeadlineIntegrationTest {
    private RedissonClient redis;
    private AnnotationConfigApplicationContext context;
    private String key;

    @BeforeAll
    void open() throws Exception {
        int port = Integer.getInteger("notify.redis.integration.port", -1);
        assertTrue(port > 0, "本票需要独占 loopback Redis");
        var properties = new RedissonProperties();
        properties.setThreads(2);
        properties.setNettyThreads(2);
        properties.setKeyPrefix("deadline-test-" + UUID.randomUUID());
        var customizer = new RedisConfig();
        var field = RedisConfig.class.getDeclaredField("redissonProperties");
        field.setAccessible(true);
        field.set(customizer, properties);
        Config config = new Config();
        customizer.redissonCustomizer().customize(config);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port)
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        redis = Redisson.create(config);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(RedissonClient.class, () -> redis);
        context.registerBean(SpringUtils.class);
        context.refresh();
        key = "owned-deadline-" + UUID.randomUUID();
    }

    @AfterAll
    void close() throws Exception {
        try {
            if (redis != null && key != null) redis.getBucket(key).delete();
        } finally {
            if (context != null) context.close();
            if (redis != null) redis.shutdown();
            for (String name : new String[]{"applicationContext", "beanFactory"}) {
                var field = SpringUtil.class.getDeclaredField(name);
                field.setAccessible(true);
                field.set(null, null);
            }
        }
    }

    @Test
    void absoluteExpirySurvivesProductionCodecAndNameMapping() {
        Instant deadline = Instant.now().plusSeconds(90).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        assertTrue(RedisUtils.setCacheObjectUntil(key, "owned-value", deadline));
        assertEquals("owned-value", RedisUtils.<String>getCacheObject(key));
        var bucket = redis.getBucket(key);
        Long expiresAt = redis.getScript(bucket.getCodec()).eval(bucket.getName(), RScript.Mode.READ_ONLY,
            "return redis.call('PEXPIRETIME', KEYS[1])", RScript.ReturnType.LONG, List.of(bucket.getName()));
        assertEquals(deadline.toEpochMilli(), expiresAt);
    }

    @Test
    void expiredAbsoluteDeadlineNeverWritesOrExtendsKey() {
        Instant originalDeadline = Instant.now().plusSeconds(90).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        assertTrue(RedisUtils.setCacheObjectUntil(key, "current-value", originalDeadline));
        var bucket = redis.getBucket(key);
        Long originalExpiry = redis.getScript(bucket.getCodec()).eval(bucket.getName(), RScript.Mode.READ_ONLY,
            "return redis.call('PEXPIRETIME', KEYS[1])", RScript.ReturnType.LONG, List.of(bucket.getName()));
        assertFalse(RedisUtils.setCacheObjectUntil(key, "late-value", Instant.now().minusSeconds(2)));
        assertEquals("current-value", RedisUtils.<String>getCacheObject(key));
        Long unchangedExpiry = redis.getScript(bucket.getCodec()).eval(bucket.getName(), RScript.Mode.READ_ONLY,
            "return redis.call('PEXPIRETIME', KEYS[1])", RScript.ReturnType.LONG, List.of(bucket.getName()));
        assertEquals(originalExpiry, unchangedExpiry);
    }
}
