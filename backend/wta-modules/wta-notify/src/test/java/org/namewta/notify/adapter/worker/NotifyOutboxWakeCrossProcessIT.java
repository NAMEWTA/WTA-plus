package org.namewta.notify.adapter.worker;

import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.NotifyOutboxClaimPort;
import org.namewta.notify.support.outbox.NotifyOutboxWakeChannels;
import org.namewta.notify.support.outbox.NotifyOutboxWakeRedisGate;
import org.namewta.notify.support.outbox.NotifyOutboxWakeSignal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AC-002：独立 Redis 客户端收到 T-01 通道唤醒后，Worker 在 &lt;1s 内 claim；不是同 JVM EventListener。
 */
@Tag("dev")
class NotifyOutboxWakeCrossProcessIT {

    @Test
    void separateRedisClientWakeClaimsWithinOneSecondWithoutLocalEvent() throws Exception {
        RedissonClient publisher = NotifyOutboxWakeRedisGate.connectOrFail("publisher");
        RedissonClient subscriber = NotifyOutboxWakeRedisGate.connectOrFail("subscriber");
        CountDownLatch claimed = new CountDownLatch(1);
        AtomicLong claimedAtNanos = new AtomicLong();
        AtomicReference<NotifyOutboxWakeSignal> received = new AtomicReference<>();
        List<String> claimOwners = new CopyOnWriteArrayList<>();
        NotifyOutboxClaimPort claim = owner -> {
            claimOwners.add(owner);
            claimedAtNanos.compareAndSet(0, System.nanoTime());
            claimed.countDown();
            return List.of();
        };
        NotifyDispatchPort dispatch = mock(NotifyDispatchPort.class);
        NotifyOutboxWorker workerB = new NotifyOutboxWorker(claim, dispatch);
        RTopic topicB = subscriber.getTopic(NotifyOutboxWakeChannels.REDIS_CHANNEL);
        int listenerId = topicB.addListener(NotifyOutboxWakeSignal.class, (channel, signal) -> {
            received.set(signal);
            workerB.onWake(signal);
        });
        try {
            long started = System.nanoTime();
            publisher.getTopic(NotifyOutboxWakeChannels.REDIS_CHANNEL)
                .publish(NotifyOutboxWakeSignal.wake(4242L));
            assertTrue(claimed.await(1, TimeUnit.SECONDS),
                "AC-002: other Redis client must drive claim within 1s via notify:outbox:wake, not 60s poll");
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(claimedAtNanos.get() - started);
            assertTrue(elapsedMs < 1000, "claim elapsed " + elapsedMs + "ms must be sub-second, not poll-delay");
            assertNotNull(received.get());
            assertEquals(NotifyOutboxWakeSignal.TYPE_WAKE, received.get().type());
            assertEquals(4242L, received.get().outboxId());
            assertEquals(1, claimOwners.size());
            verify(dispatch, never()).dispatch(org.mockito.ArgumentMatchers.any(NotifyOutbox.class));
        } finally {
            topicB.removeListener(listenerId);
            publisher.shutdown();
            subscriber.shutdown();
        }
    }
}
