package org.namewta.common.redis.aspectj;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.namewta.common.core.constant.GlobalConstants;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.common.redis.config.RedisConfig;
import org.namewta.common.redis.config.properties.RedissonProperties;
import org.namewta.common.redis.utils.RedisUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RBucket;
import org.redisson.config.Config;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 使用生产序列化/NameMapper和真实Spring AOP代理验证租约所有权，不连接现有Redis。 */
@Tag("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepeatSubmitRedisIntegrationTest {
    private RedissonClient redis;
    private AnnotationConfigApplicationContext context;
    private final ThreadLocal<Callable<Object>> actions = new ThreadLocal<>();
    private final AtomicInteger bodyCalls = new AtomicInteger();
    private final AtomicInteger comparisons = new AtomicInteger();
    private final AtomicBoolean failRelease = new AtomicBoolean();
    private final AtomicBoolean failAcquire = new AtomicBoolean();
    private final Set<String> keys = new LinkedHashSet<>();
    private Fixture proxy;

    @BeforeAll
    void start() throws Exception {
        int port = Integer.getInteger("repeat.redis.integration.port", -1);
        assumeTrue(port > 0, "owned isolated Redis required");
        var properties = new RedissonProperties();
        properties.setThreads(2);
        properties.setNettyThreads(2);
        properties.setKeyPrefix("repeat-test-" + UUID.randomUUID());
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
        context.registerBean(RedissonClient.class, this::faultInjectableClient);
        context.registerBean(SpringUtils.class);
        context.refresh();
        var factory = new AspectJProxyFactory(new Fixture(actions, bodyCalls));
        factory.addAspect(new RepeatSubmitAspect());
        proxy = factory.getProxy();
    }

    @AfterAll
    void stop() throws Exception {
        try {
            if (redis != null) {
                for (String key : keys) {
                    redis.getBucket(key).delete();
                }
            }
        } finally {
            if (context != null) {
                context.close();
            }
            if (redis != null) {
                redis.shutdown();
            }
            RequestContextHolder.resetRequestAttributes();
            actions.remove();
            for (String name : new String[]{"applicationContext", "beanFactory"}) {
                var field = SpringUtil.class.getDeclaredField(name);
                field.setAccessible(true);
                field.set(null, null);
            }
        }
    }

    @Test
    void expiredRequestCannotDeleteNewOwnerLease() throws Exception {
        String path = path();
        String key = key(path);
        var entered = new CountDownLatch(1);
        var failA = new CountDownLatch(1);
        var failure = new IllegalStateException("owned-test-A-failed");
        try (var executor = Executors.newSingleThreadExecutor()) {
            try {
                var a = executor.submit(() -> call(path, () -> {
                    entered.countDown();
                    assertTrue(failA.await(5, TimeUnit.SECONDS));
                    throw failure;
                }));
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertTrue(redis.getBucket(key).isExists());
                // 只缩短本测试已拥有租约的TTL，由真实Redis使其到期；不DEL来模拟过期。
                assertTrue(redis.getBucket(key).expire(Duration.ofMillis(50)));
                awaitExpiry(key);
                assertInstanceOf(R.class, call(path, R::ok));
                Object ownerB = redis.getBucket(key).get();
                assertNotNull(ownerB);
                failA.countDown();
                ExecutionException rejected = assertThrows(ExecutionException.class, () -> a.get(5, TimeUnit.SECONDS));
                assertSame(failure, rejected.getCause());
                assertEquals(ownerB, redis.getBucket(key).get(), "A must not delete B's lease");
                int beforeC = bodyCalls.get();
                assertThrows(ServiceException.class, () -> call(path, R::ok));
                assertEquals(beforeC, bodyCalls.get(), "C must not enter business code");
                // 复用执行A的同一个线程，再次获取失败也不能影响B。
                var reused = executor.submit(() -> assertThrows(ServiceException.class, () -> call(path, R::ok)));
                reused.get(5, TimeUnit.SECONDS);
                assertEquals(ownerB, redis.getBucket(key).get());
            } finally {
                failA.countDown();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void unsuccessfulResultReleasesOnlyItsOwnLeaseAndCanRetry() throws Exception {
        String path = path();
        var failed = R.fail("business failure");
        assertSame(failed, call(path, () -> failed));
        assertFalse(redis.getBucket(key(path)).isExists());
        assertInstanceOf(R.class, call(path, R::ok));
        assertTrue(redis.getBucket(key(path)).isExists());
    }

    @Test
    void businessExceptionAndErrorReleaseWithoutThreadReuseResidue() throws Exception {
        String path = path();
        var failure = new IllegalArgumentException("business failure");
        assertSame(failure, assertThrows(IllegalArgumentException.class, () -> call(path, () -> { throw failure; })));
        assertFalse(redis.getBucket(key(path)).isExists());
        var error = new AssertionError("test application error");
        assertSame(error, assertThrows(AssertionError.class, () -> call(path, () -> { throw error; })));
        assertFalse(redis.getBucket(key(path)).isExists());
        assertInstanceOf(R.class, call(path, R::ok));
        assertThrows(ServiceException.class, () -> call(path, R::ok));
    }

    @Test
    void successfulAndNonRResultsKeepOriginalWindow() throws Exception {
        for (Object result : new Object[]{R.ok(), "stream-result", null}) {
            String path = path();
            assertSame(result, call(path, () -> result));
            long before = redis.getBucket(key(path)).remainTimeToLive();
            assertTrue(before > 0 && before <= 5000);
            int calls = bodyCalls.get();
            assertThrows(ServiceException.class, () -> call(path, R::ok));
            assertEquals(calls, bodyCalls.get());
            long after = redis.getBucket(key(path)).remainTimeToLive();
            assertTrue(after > 0 && after <= before, "reject must not refresh the window");
        }
    }

    @Test
    void invalidIntervalNeverAcquiresOrCallsBusinessCode() {
        String path = path();
        int calls = bodyCalls.get();
        request(path);
        try {
            assertThrows(ServiceException.class, () -> proxy.invalid("same"));
            assertEquals(calls, bodyCalls.get());
            assertFalse(redis.getBucket(key(path)).isExists());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void compareDeleteUsesProductionCodecAndCannotDeleteAnotherValue() {
        String key = key(path());
        assertTrue(RedisUtils.setObjectIfAbsent(key, "owner-A", Duration.ofSeconds(5)));
        long ttl = redis.getBucket(key).remainTimeToLive();
        assertFalse(RedisUtils.deleteObjectIfEquals(key, "owner-B"));
        assertEquals("owner-A", redis.getBucket(key).get());
        assertTrue(redis.getBucket(key).remainTimeToLive() <= ttl);
        assertThrows(NullPointerException.class, () -> RedisUtils.deleteObjectIfEquals(key, null));
        assertEquals("owner-A", redis.getBucket(key).get());
        assertTrue(RedisUtils.deleteObjectIfEquals(key, "owner-A"));
        assertFalse(RedisUtils.deleteObjectIfEquals(key, "owner-A"));
    }

    @Test
    void releaseFailurePreservesBusinessFailureAndAcquireFailureNeverReleases() {
        String path = path();
        var businessFailure = new IllegalStateException("business failure survives");
        failRelease.set(true);
        try {
            assertSame(businessFailure, assertThrows(IllegalStateException.class,
                () -> call(path, () -> { throw businessFailure; })));
            assertEquals(1, businessFailure.getSuppressed().length);
            assertEquals("injected release failure", businessFailure.getSuppressed()[0].getMessage());
            assertTrue(redis.getBucket(key(path)).remainTimeToLive() > 0, "failed cleanup relies on original TTL");
        } finally {
            failRelease.set(false);
        }
        String failedAcquirePath = path();
        int comparedBefore = comparisons.get();
        int calledBefore = bodyCalls.get();
        failAcquire.set(true);
        try {
            assertThrows(IllegalStateException.class, () -> call(failedAcquirePath, R::ok));
            assertEquals(calledBefore, bodyCalls.get());
            assertEquals(comparedBefore, comparisons.get());
            assertFalse(redis.getBucket(key(failedAcquirePath)).isExists());
        } finally {
            failAcquire.set(false);
        }
    }

    private RedissonClient faultInjectableClient() {
        return (RedissonClient) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{RedissonClient.class},
            (proxy, method, args) -> {
                try {
                    Object result = method.invoke(redis, args);
                    if (result instanceof RBucket<?> bucket) {
                        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{RBucket.class},
                            (bucketProxy, bucketMethod, bucketArgs) -> {
                                if (bucketMethod.getName().equals("setIfAbsent") && failAcquire.get()) {
                                    throw new IllegalStateException("injected acquire failure");
                                }
                                if (bucketMethod.getName().equals("compareAndSet")) {
                                    comparisons.incrementAndGet();
                                    if (failRelease.get()) {
                                        throw new IllegalStateException("injected release failure");
                                    }
                                }
                                try {
                                    return bucketMethod.invoke(bucket, bucketArgs);
                                } catch (InvocationTargetException failure) {
                                    throw failure.getCause();
                                }
                            });
                    }
                    return result;
                } catch (InvocationTargetException failure) {
                    throw failure.getCause();
                }
            });
    }

    private Object call(String path, Callable<Object> action) throws Exception {
        request(path);
        actions.set(action);
        try {
            return proxy.call("same");
        } finally {
            actions.remove();
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private void request(String path) {
        var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getRequestURI" -> path;
                case "getParameterMap" -> Map.of();
                default -> null;
            });
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private String path() {
        String path = "/test/repeat/" + UUID.randomUUID();
        keys.add(key(path));
        return path;
    }

    private String key(String path) {
        return GlobalConstants.REPEAT_SUBMIT_KEY + path + SecureUtil.md5(":" + JsonUtils.toJsonString("same"));
    }

    private void awaitExpiry(String key) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (redis.getBucket(key).isExists() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertFalse(redis.getBucket(key).isExists(), "Redis lease must expire before B starts");
    }

    public static class Fixture {
        private final ThreadLocal<Callable<Object>> actions;
        private final AtomicInteger calls;

        public Fixture(ThreadLocal<Callable<Object>> actions, AtomicInteger calls) {
            this.actions = actions;
            this.calls = calls;
        }

        @RepeatSubmit(interval = 5000, message = "repeat-denied")
        public Object call(String payload) throws Exception {
            calls.incrementAndGet();
            return actions.get().call();
        }

        @RepeatSubmit(interval = 1, message = "invalid-window")
        public Object invalid(String payload) {
            calls.incrementAndGet();
            return R.ok();
        }
    }
}
