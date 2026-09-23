package org.namewta.test.notify.idempotency;

import org.namewta.common.notify.idempotency.NotifyIdempotencyStore;
import org.namewta.common.notify.idempotency.RedisNotifyIdempotencyStore;
import org.namewta.common.notify.model.*;
import org.namewta.common.redis.handler.KeyPrefixHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.codec.TypedJsonJackson3Codec;
import org.redisson.config.Config;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实 Redis 通知幂等状态机验证。
 */
@Tag("dev")
class RedisNotifyIdempotencyStoreIntegrationTest {

    private RedissonClient client;

    @AfterEach
    void shutdown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void shouldAtomicallyAcquireCompleteReuseAndExpire() throws Exception {
        int port = Integer.getInteger("notify.redis.integration.port", -1);
        Assumptions.assumeTrue(port > 0, "需要一次性 Redis 端口");
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        client = Redisson.create(config);
        NotifyIdempotencyStore store = new RedisNotifyIdempotencyStore(client);
        String storageKey = "notify:idempotency:v1:" + UUID.randomUUID();
        String digest = "digest-a";
        Duration window = Duration.ofMinutes(5);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<NotifyIdempotencyStore.Claim>> tasks = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> (Callable<NotifyIdempotencyStore.Claim>) () ->
                    store.acquire(storageKey, digest, "request-" + index, window))
                .toList();
            List<NotifyIdempotencyStore.Claim> claims = executor.invokeAll(tasks).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }).toList();
            assertEquals(1, claims.stream().filter(NotifyIdempotencyStore.Acquired.class::isInstance).count());
            assertEquals(19, claims.stream().filter(NotifyIdempotencyStore.InProgress.class::isInstance).count());

            NotifyIdempotencyStore.Acquired acquired = claims.stream()
                .filter(NotifyIdempotencyStore.Acquired.class::isInstance)
                .map(NotifyIdempotencyStore.Acquired.class::cast)
                .findFirst().orElseThrow();
            NotifyTarget target = NotifyTarget.phone("13800000000");
            NotifyResult result = new NotifyResult(acquired.requestId(), NotifyChannel.SMS, "provider-a",
                NotifyStatus.ACCEPTED,
                List.of(NotifyTargetResult.accepted(target, "message-1", 1L)));
            store.complete(acquired, result);

            NotifyIdempotencyStore.Claim completed = store.acquire(storageKey, digest, "request-next", window);
            NotifyIdempotencyStore.Claim conflict = store.acquire(storageKey, "digest-b", "request-conflict", window);

            assertInstanceOf(NotifyIdempotencyStore.Completed.class, completed);
            assertEquals(result, ((NotifyIdempotencyStore.Completed) completed).result());
            assertInstanceOf(NotifyIdempotencyStore.Conflict.class, conflict);
            long ttl = client.getBucket(storageKey).remainTimeToLive();
            assertTrue(ttl > Duration.ofMinutes(4).toMillis());
            assertTrue(ttl <= window.toMillis());
        } finally {
            client.getBucket(storageKey).delete();
        }
    }

    @Test
    void retryableOwnerCasRetainsTtlAndRejectsStaleOwnerWithStringCodec() throws Exception {
        retryableOwnerScenario(false);
    }

    @Test
    void retryableOwnerCasRetainsTtlAndReadsLegacyStateWithProductionCompositeCodec() throws Exception {
        retryableOwnerScenario(true);
    }

    private void retryableOwnerScenario(boolean productionCodec) throws Exception {
        int port = Integer.getInteger("notify.redis.integration.port", -1);
        Assumptions.assumeTrue(port > 0, "需要一次性 Redis 端口");
        Config config = new Config();
        if (productionCodec) {
            var valueCodec = new TypedJsonJackson3Codec(Object.class, JsonMapper.builder().build());
            config.setCodec(new CompositeCodec(StringCodec.INSTANCE, valueCodec, valueCodec));
            config.setNameMapper(new KeyPrefixHandler("owned-t37-" + UUID.randomUUID()));
        } else config.setCodec(StringCodec.INSTANCE);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        client = Redisson.create(config);
        NotifyIdempotencyStore store = new RedisNotifyIdempotencyStore(client);
        String key = "notify:idempotency:v1:" + UUID.randomUUID();
        Duration window = Duration.ofMinutes(5);
        var bucket = client.<String>getBucket(key);
        try {
            NotifyIdempotencyStore.Acquired first = assertInstanceOf(NotifyIdempotencyStore.Acquired.class,
                store.acquire(key, "digest-a", "same-request", window));
            assertInstanceOf(NotifyIdempotencyStore.InProgress.class,
                store.acquire(key, "digest-a", "same-request", window));
            long before = bucket.remainTimeToLive();
            store.markRetryable(first);
            long after = bucket.remainTimeToLive();
            assertTrue(after > 0 && after <= before, "markRetryable must keep the original expiry");
            assertInstanceOf(NotifyIdempotencyStore.Conflict.class,
                store.acquire(key, "digest-b", "other", window));

            AtomicInteger acquired = new AtomicInteger();
            List<NotifyIdempotencyStore.Claim> claims;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var tasks = java.util.stream.IntStream.range(0, 20)
                    .mapToObj(index -> (Callable<NotifyIdempotencyStore.Claim>) () ->
                        store.acquire(key, "digest-a", "same-request", window)).toList();
                claims = executor.invokeAll(tasks).stream().map(future -> {
                    try { return future.get(); } catch (Exception e) { throw new IllegalStateException(e); }
                }).toList();
            }
            claims.forEach(claim -> { if (claim instanceof NotifyIdempotencyStore.Acquired) acquired.incrementAndGet(); });
            assertEquals(1, acquired.get());
            assertEquals(19, claims.stream().filter(NotifyIdempotencyStore.InProgress.class::isInstance).count());
            NotifyIdempotencyStore.Acquired second = claims.stream()
                .filter(NotifyIdempotencyStore.Acquired.class::isInstance)
                .map(NotifyIdempotencyStore.Acquired.class::cast).findFirst().orElseThrow();
            assertNotEquals(first.expectedValue(), second.expectedValue(), "same requestId still needs a new owner nonce");
            assertThrows(IllegalStateException.class, () -> store.complete(first, accepted(first.requestId())));
            assertThrows(IllegalStateException.class, () -> store.markRetryable(first));
            store.release(first);
            assertInstanceOf(NotifyIdempotencyStore.InProgress.class,
                store.acquire(key, "digest-a", "same-request", window));
            assertTrue(bucket.remainTimeToLive() <= after, "retry acquire must not reset the window");
            store.complete(second, accepted(second.requestId()));
            assertInstanceOf(NotifyIdempotencyStore.Completed.class,
                store.acquire(key, "digest-a", "same-request", window));
            assertTrue(bucket.remainTimeToLive() > Duration.ofMinutes(4).toMillis(),
                "completion restores the established completed-state window");
            store.release(second);
            assertInstanceOf(NotifyIdempotencyStore.Completed.class,
                store.acquire(key, "digest-a", "same-request", window));

            // Redisson 4.6.1 的 Bucket Args CAS 使用未映射名；正例须在 NameMapper 下证明当前 owner 可操作。
            String freshKey = key + ":fresh";
            String releaseKey = key + ":release";
            try {
                NotifyIdempotencyStore.Acquired fresh = assertInstanceOf(NotifyIdempotencyStore.Acquired.class,
                    store.acquire(freshKey, "digest-fresh", "fresh-request", window));
                store.complete(fresh, accepted(fresh.requestId()));
                assertInstanceOf(NotifyIdempotencyStore.Completed.class,
                    store.acquire(freshKey, "digest-fresh", "fresh-request", window));

                NotifyIdempotencyStore.Acquired releasable = assertInstanceOf(NotifyIdempotencyStore.Acquired.class,
                    store.acquire(releaseKey, "digest-release", "release-request", window));
                store.release(releasable);
                assertFalse(client.getBucket(releaseKey).isExists(), "current owner release must delete its exact key");
                assertInstanceOf(NotifyIdempotencyStore.Acquired.class,
                    store.acquire(releaseKey, "digest-release", "release-request", window));
            } finally {
                client.getBucket(freshKey).delete();
                client.getBucket(releaseKey).delete();
            }

            if (productionCodec) {
                // 四字段旧缓存无 owner；不能按旧 FAILED/errorCode 推断安全重发。
                String legacy = JsonMapper.builder().build().writeValueAsString(
                    new RedisNotifyIdempotencyStore.StoredState("COMPLETED", "digest-a", "legacy",
                        new NotifyResult("legacy", NotifyChannel.SMS, "provider", NotifyStatus.FAILED,
                            List.of(NotifyTargetResult.failed(NotifyTarget.phone("13800000000"),
                                "PROVIDER_REJECTED", "old failure", 1L))), null))
                    .replace(",\"ownerNonce\":null", "");
                bucket.set(legacy, window);
                NotifyIdempotencyStore.Completed old = assertInstanceOf(NotifyIdempotencyStore.Completed.class,
                    store.acquire(key, "digest-a", "same-request", window));
                assertEquals(NotifyDeliveryStatus.FAILED, old.result().deliveries().getFirst().status());
                bucket.set("{\"state\":\"IN_PROGRESS\",\"digest\":\"digest-a\","
                    + "\"requestId\":\"legacy\",\"result\":null}", window);
                assertInstanceOf(NotifyIdempotencyStore.InProgress.class,
                    store.acquire(key, "digest-a", "same-request", window));
                bucket.set("not-json", window);
                assertThrows(RuntimeException.class, () -> store.acquire(key, "digest-a", "same-request", window));
            }
        } finally {
            bucket.delete();
        }
    }

    private NotifyResult accepted(String requestId) {
        return new NotifyResult(requestId, NotifyChannel.SMS, "provider", NotifyStatus.ACCEPTED,
            List.of(NotifyTargetResult.accepted(NotifyTarget.phone("13800000000"), "owned-message", 1L)));
    }
}
