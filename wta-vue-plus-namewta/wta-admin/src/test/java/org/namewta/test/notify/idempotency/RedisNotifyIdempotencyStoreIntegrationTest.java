package org.namewta.test.notify.idempotency;

import org.namewta.common.notify.idempotency.NotifyIdempotencyStore;
import org.namewta.common.notify.idempotency.RedisNotifyIdempotencyStore;
import org.namewta.common.notify.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

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
}
