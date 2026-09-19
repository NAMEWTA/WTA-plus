package org.namewta.notify.adapter.worker;

import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.NotifyOutboxClaimPort;
import org.namewta.notify.support.outbox.NotifyOutboxWakeChannels;
import org.namewta.notify.support.outbox.NotifyOutboxWakeSignal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Worker 默认 60s 兜底，唤醒与定时共用 claim+dispatch，且不按 outboxId 直投。
 */
@Tag("dev")
class NotifyOutboxWorkerWakePollTest {

    @Test
    void defaultPollDelayIsSixtySecondsNotOneSecond() throws Exception {
        Scheduled scheduled = NotifyOutboxWorker.class.getMethod("poll").getAnnotation(Scheduled.class);
        assertNotNull(scheduled);
        assertEquals("${notify.outbox.poll-delay-ms:60000}", scheduled.fixedDelayString());
        assertFalse(scheduled.fixedDelayString().contains(":1000}"));
    }

    @Test
    void overlappingWakeStormIsCoalescedAndPollWorksAfterCompletion() throws Exception {
        Fixture fixture = fixture();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        NotifyOutbox due = outbox(17L);
        when(fixture.claim.claim(anyString())).thenReturn(List.of(due), List.of());
        org.mockito.Mockito.doAnswer(invocation -> {
            entered.countDown();
            try { assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw failure; }
            return null;
        }).when(fixture.dispatch).dispatch(due);
        try (var consumer = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var running = consumer.submit(fixture.worker::poll);
            try {
                assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
                for (int index = 0; index < 1000; index++) fixture.worker.onWake(NotifyOutboxWakeSignal.wake(17L));
                verify(fixture.claim, times(1)).claim(anyString());
            } finally { release.countDown(); }
            running.get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        fixture.worker.poll();
        verify(fixture.claim, times(3)).claim(anyString());
        verify(fixture.dispatch, times(1)).dispatch(due);
    }

    @Test
    void failedDrainReleasesTheCoalescingFlagForPollRecovery() {
        Fixture fixture = fixture();
        when(fixture.claim.claim(anyString())).thenThrow(new IllegalStateException("owned claim failure")).thenReturn(List.of());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, fixture.worker::poll);
        assertDoesNotThrow(fixture.worker::poll);
        verify(fixture.claim, times(2)).claim(anyString());
    }

    @Test
    void wakeAndScheduledShareClaimDispatchLoopAndSameOwner() {
        Fixture fixture = fixture();
        NotifyOutbox due = outbox(11L);
        when(fixture.claim.claim(anyString())).thenReturn(List.of(due), List.of(), List.of(due), List.of());

        fixture.worker.poll();
        fixture.worker.onWake(NotifyOutboxWakeSignal.wake(11L));

        ArgumentCaptor<String> owners = ArgumentCaptor.forClass(String.class);
        verify(fixture.claim, times(4)).claim(owners.capture());
        assertEquals(1, Set.copyOf(owners.getAllValues()).size());
        verify(fixture.dispatch, times(2)).dispatch(due);
    }

    @Test
    void drainContinuesClaimUntilEmptyBatch() {
        Fixture fixture = fixture();
        NotifyOutbox first = outbox(1L);
        NotifyOutbox second = outbox(2L);
        when(fixture.claim.claim(anyString())).thenReturn(List.of(first), List.of(second), List.of());

        fixture.worker.poll();

        verify(fixture.claim, times(3)).claim(anyString());
        verify(fixture.dispatch).dispatch(first);
        verify(fixture.dispatch).dispatch(second);
    }

    @Test
    void suppressedWakeStillClaimsOnScheduledPoll() {
        Fixture fixture = fixture();
        NotifyOutbox due = outbox(21L);
        when(fixture.claim.claim(anyString())).thenReturn(List.of(due), List.of());

        fixture.worker.poll();

        verify(fixture.claim, times(2)).claim(anyString());
        verify(fixture.dispatch).dispatch(due);
        verify(fixture.dispatch, never()).dispatch(outbox(99L));
    }

    @Test
    void wakeDoesNotDirectDispatchFutureNextAttemptHint() {
        Fixture fixture = fixture();
        when(fixture.claim.claim(anyString())).thenReturn(List.of());

        fixture.worker.onWake(NotifyOutboxWakeSignal.wake(99L));

        verify(fixture.claim).claim(anyString());
        verify(fixture.dispatch, never()).dispatch(any());
    }

    @Test
    void subscriberBindsT01ChannelAndForwardsWakeWithoutBypass() {
        Fixture fixture = fixture();
        when(fixture.claim.claim(anyString())).thenReturn(List.of());
        RecordingBus bus = new RecordingBus();
        NotifyOutboxWakeSubscriber subscriber = new NotifyOutboxWakeSubscriber(fixture.worker, bus);

        subscriber.afterPropertiesSet();
        assertEquals(NotifyOutboxWakeChannels.REDIS_CHANNEL, bus.channel);
        assertEquals(NotifyOutboxWakeSignal.class, bus.type);
        bus.emit(NotifyOutboxWakeSignal.wake(99L));
        verify(fixture.claim).claim(anyString());
        verify(fixture.dispatch, never()).dispatch(any());

        subscriber.destroy();
        assertEquals(Integer.valueOf(7), bus.unsubscribedId);
    }

    @Test
    void subscribeFailureDoesNotPreventConstruction() {
        NotifyOutboxWorker worker = new NotifyOutboxWorker(mock(NotifyOutboxClaimPort.class), mock(NotifyDispatchPort.class));
        NotifyOutboxWakeBus failing = new NotifyOutboxWakeBus() {
            @Override
            public int subscribe(String channel, Class<NotifyOutboxWakeSignal> type,
                                 Consumer<NotifyOutboxWakeSignal> consumer) {
                throw new IllegalStateException("redis down");
            }

            @Override
            public void unsubscribe(String channel, int listenerId) {
            }
        };
        NotifyOutboxWakeSubscriber subscriber = new NotifyOutboxWakeSubscriber(worker, failing);
        assertDoesNotThrow(subscriber::afterPropertiesSet);
        assertDoesNotThrow(subscriber::destroy);
    }

    private static Fixture fixture() {
        NotifyOutboxClaimPort claim = mock(NotifyOutboxClaimPort.class);
        NotifyDispatchPort dispatch = mock(NotifyDispatchPort.class);
        return new Fixture(new NotifyOutboxWorker(claim, dispatch), claim, dispatch);
    }

    private static NotifyOutbox outbox(long id) {
        NotifyOutbox outbox = new NotifyOutbox();
        outbox.setOutboxId(id);
        outbox.setStatus("READY");
        return outbox;
    }

    private record Fixture(NotifyOutboxWorker worker, NotifyOutboxClaimPort claim, NotifyDispatchPort dispatch) {
    }

    private static final class RecordingBus implements NotifyOutboxWakeBus {
        private String channel;
        private Class<NotifyOutboxWakeSignal> type;
        private Consumer<NotifyOutboxWakeSignal> consumer;
        private Integer unsubscribedId;
        private final AtomicInteger nextId = new AtomicInteger(7);

        @Override
        public int subscribe(String channel, Class<NotifyOutboxWakeSignal> type,
                             Consumer<NotifyOutboxWakeSignal> consumer) {
            this.channel = channel;
            this.type = type;
            this.consumer = consumer;
            return nextId.get();
        }

        @Override
        public void unsubscribe(String channel, int listenerId) {
            this.unsubscribedId = listenerId;
        }

        private void emit(NotifyOutboxWakeSignal signal) {
            consumer.accept(signal);
        }
    }
}
