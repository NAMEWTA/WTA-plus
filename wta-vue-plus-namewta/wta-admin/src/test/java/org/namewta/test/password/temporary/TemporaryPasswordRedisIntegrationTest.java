package org.namewta.test.password.temporary;

import org.namewta.system.temporarypassword.RedisTemporaryPasswordStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class TemporaryPasswordRedisIntegrationTest {

    private RedissonClient client;

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.getKeys().deleteByPattern("auth:temporary-password:user:*");
            client.shutdown();
        }
    }

    @Test
    void overwriteExpiryAndCompareDeleteAreAtomicOnRealRedis() throws Exception {
        int port = Integer.getInteger("temporary.password.redis.integration.port", -1);
        Assumptions.assumeTrue(port > 0, "requires a disposable Redis port");
        client = client(port);
        RedisTemporaryPasswordStore store = new RedisTemporaryPasswordStore(client);
        Long userId = 42L;

        store.store(userId, "hash-one", Duration.ofSeconds(60));
        long ttl = client.getBucket(RedisTemporaryPasswordStore.key(userId)).remainTimeToLive();
        assertTrue(ttl > 0 && ttl <= 60_000);
        store.store(userId, "hash-two", Duration.ofSeconds(60));
        assertEquals("hash-two", store.read(userId));
        assertFalse(store.compareAndDelete(userId, "hash-one"));
        assertEquals("hash-two", store.read(userId));

        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = List.of(
                executor.submit(() -> awaitAndConsume(start, store, userId)),
                executor.submit(() -> awaitAndConsume(start, store, userId)));
            start.countDown();
            long successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successes++;
                }
            }
            assertEquals(1, successes);
        }
        assertNull(store.read(userId));

        store.store(userId, "expiring-hash", Duration.ofMillis(150));
        Thread.sleep(300);
        assertNull(store.read(userId));
    }

    private static boolean awaitAndConsume(CountDownLatch start, RedisTemporaryPasswordStore store, Long userId)
        throws InterruptedException {
        start.await();
        return store.compareAndDelete(userId, "hash-two");
    }

    private static RedissonClient client(int port) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        return Redisson.create(config);
    }
}
