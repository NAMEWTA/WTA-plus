package org.namewta.test.oss.upload;

import org.namewta.system.oss.upload.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 一次性真实 Redis Ticket、索引和分布式锁验证。
 */
@Tag("dev")
class RedisOssUploadTicketStoreIntegrationTest {

    private RedissonClient client;

    @AfterEach
    void shutdown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void shouldRoundTripIndexAndSerializeConcurrentLock() throws Exception {
        int port = Integer.getInteger("oss.upload.redis.integration.port", -1);
        Assumptions.assumeTrue(port > 0, "需要一次性 Redis 端口");
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
        client = Redisson.create(config);
        RedisOssUploadTicketStore store = new RedisOssUploadTicketStore(client);
        String token = UUID.randomUUID().toString();
        long expiresAt = System.currentTimeMillis() + 1000;
        OssUploadTicket ticket = new OssUploadTicket(token, "general", OssUploadMode.MULTIPART,
            OssUploadState.INITIALIZED, "service-a", "bucket-a", "key-a", "upload-a", "a.bin", ".bin",
            8, "application/octet-stream", "fp", "digest", 7L, 100L, 5, 2,
            System.currentTimeMillis(), expiresAt, null);
        OssUploadCleanupRecord cleanup = new OssUploadCleanupRecord(token, OssUploadMode.MULTIPART,
            "service-a", "key-a", "upload-a", expiresAt);

        try {
            store.create(ticket, cleanup, Duration.ofMinutes(5), Duration.ofMinutes(10));
            assertEquals(ticket, store.get(token));
            assertEquals(cleanup, store.getCleanup(token));
            assertEquals(List.of(token), store.findExpired(expiresAt, 10));

            AtomicInteger active = new AtomicInteger();
            AtomicInteger maximum = new AtomicInteger();
            List<Callable<Void>> calls = new ArrayList<>();
            for (int index = 0; index < 12; index++) {
                calls.add(() -> store.locked(token, () -> {
                    int current = active.incrementAndGet();
                    maximum.accumulateAndGet(current, Math::max);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        active.decrementAndGet();
                    }
                    return null;
                }));
            }
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executor.invokeAll(calls).forEach(future -> assertDoesNotThrow(() -> {
                    future.get();
                }));
            }
            assertEquals(1, maximum.get());
            store.removeCompletedCleanup(token);
            assertNull(store.getCleanup(token));
        } finally {
            store.removeSession(token);
        }
    }
}
