package org.namewta.notify.adapter.event;

import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;

import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import com.baomidou.dynamic.datasource.tx.DsTxEventListenerFactory;
import com.baomidou.dynamic.datasource.tx.TransactionContext;
import org.namewta.notify.api.NotificationCancelCommand;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationRetryCommand;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.entity.NotifyRecipient;
import org.namewta.notify.support.outbox.NotifyOutboxWakeChannels;
import org.namewta.notify.support.outbox.NotifyOutboxWakeRequestedEvent;
import org.namewta.notify.support.outbox.NotifyOutboxWakeSignal;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.event.TransactionPhase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提交后跨进程唤醒：提交前不发、提交后发、Redis 失败不回滚、载荷无 PII。
 */
@Tag("dev")
class NotifyOutboxWakePublisherTest {

    @Test
    void stalledRedisPublishIsBoundedAndCancelled() {
        var pending = new java.util.concurrent.CompletableFuture<Long>();
        var publisher = new NotifyOutboxWakePublisher((channel, signal) -> pending, 20);
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(1),
            () -> publisher.publishAfterCommit(new NotifyOutboxWakeRequestedEvent(1L)));
        assertTrue(pending.isCancelled());
    }

    @Test
    void interruptedPublishPreservesInterruptAndCancelsPendingOperation() {
        var pending = new java.util.concurrent.CompletableFuture<Long>();
        var publisher = new NotifyOutboxWakePublisher((channel, signal) -> pending, 250);
        Thread.currentThread().interrupt();
        try {
            publisher.publishAfterCommit(new NotifyOutboxWakeRequestedEvent(1L));
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(pending.isCancelled());
        } finally { Thread.interrupted(); }
    }

    @Test
    void committingThreadDoesNotWaitForLocalProvider() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        var releaseProvider = new java.util.concurrent.CountDownLatch(1);
        var providerCalls = new java.util.concurrent.atomic.AtomicInteger();
        org.namewta.notify.port.NotifyOutboxClaimPort claims = owner -> {
            NotifyOutbox row = new NotifyOutbox(); row.setOutboxId(77L);
            return providerCalls.get() == 0 ? List.of(row) : List.of();
        };
        org.namewta.notify.port.NotifyDispatchPort dispatch = new org.namewta.notify.port.NotifyDispatchPort() {
            public void dispatch(NotifyOutbox row) {
                providerCalls.incrementAndGet();
                try { releaseProvider.await(3, java.util.concurrent.TimeUnit.SECONDS); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
            }
            public void refreshAggregate(Long id) { }
        };
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(org.namewta.notify.adapter.worker.NotifyOutboxWorker.class,
                () -> new org.namewta.notify.adapter.worker.NotifyOutboxWorker(claims, dispatch));
            context.registerBean(NotifyOutboxWakePublisher.class, () -> new NotifyOutboxWakePublisher(transport, 250));
            context.refresh();
            try (var caller = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                var committed = caller.submit(() -> {
                    TransactionContext.bind("owned-t28-commit");
                    try {
                        context.publishEvent(new NotifyOutboxWakeRequestedEvent(77L));
                        TransactionContext.getSynchronizations().getFirst().afterCommit();
                        return true;
                    } finally { TransactionContext.removeSynchronizations(); TransactionContext.remove(); }
                });
                try {
                    assertTrue(committed.get(500, java.util.concurrent.TimeUnit.MILLISECONDS));
                    assertEquals(0, providerCalls.get(), "default local multicaster must not call the Provider on the commit thread");
                } finally { releaseProvider.countDown(); }
            }
        }
    }

    @Test
    void listenerUsesExplicitAfterCommitPhase() throws Exception {
        Method listener = NotifyOutboxWakePublisher.class.getMethod(
            "publishAfterCommit", NotifyOutboxWakeRequestedEvent.class);
        DsTxEventListener annotation = listener.getAnnotation(DsTxEventListener.class);
        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }

    @Test
    void doesNotPublishOnRedisUntilDynamicDatasourceTransactionCommits() {
        RecordingTransport transport = new RecordingTransport();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(NotifyOutboxWakePublisher.class,
                () -> new NotifyOutboxWakePublisher(transport, 250));
            context.refresh();
            TransactionContext.bind("notify-outbox-wake-test");
            try {
                context.publishEvent(new NotifyOutboxWakeRequestedEvent(42L));
                assertTrue(transport.published.isEmpty());
                assertEquals(1, TransactionContext.getSynchronizations().size());
                TransactionContext.getSynchronizations().getFirst().afterCommit();
                assertEquals(1, transport.published.size());
                PublishedWake wake = transport.published.getFirst();
                assertEquals(NotifyOutboxWakeChannels.REDIS_CHANNEL, wake.channel());
                assertEquals(NotifyOutboxWakeSignal.TYPE_WAKE, wake.signal().type());
                assertEquals(42L, wake.signal().outboxId());
            } finally {
                TransactionContext.removeSynchronizations();
                TransactionContext.remove();
            }
        }
    }

    @Test
    void redisPublishFailureDoesNotEscapeAfterCommit() {
        RecordingTransport transport = new RecordingTransport();
        transport.failure = new IllegalStateException("redis down");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(NotifyOutboxWakePublisher.class,
                () -> new NotifyOutboxWakePublisher(transport, 250));
            context.refresh();
            TransactionContext.bind("notify-outbox-wake-redis-fail");
            try {
                context.publishEvent(new NotifyOutboxWakeRequestedEvent(7L));
                assertDoesNotThrow(() -> TransactionContext.getSynchronizations().getFirst().afterCommit());
                assertTrue(transport.published.isEmpty());
            } finally {
                TransactionContext.removeSynchronizations();
                TransactionContext.remove();
            }
        }
    }

    @Test
    void wakePayloadOmitsPiiBodyAndSecret() {
        RecordingTransport transport = new RecordingTransport();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(NotifyOutboxWakePublisher.class,
                () -> new NotifyOutboxWakePublisher(transport, 250));
            context.refresh();
            TransactionContext.bind("notify-outbox-wake-pii");
            try {
                context.publishEvent(new NotifyOutboxWakeRequestedEvent(99L));
                TransactionContext.getSynchronizations().getFirst().afterCommit();
            } finally {
                TransactionContext.removeSynchronizations();
                TransactionContext.remove();
            }
        }
        NotifyOutboxWakeSignal signal = transport.published.getFirst().signal();
        String rendered = signal.toString();
        assertFalse(rendered.contains("13800138000"));
        assertFalse(rendered.contains("secret-user@example.com"));
        assertFalse(rendered.contains("通知正文"));
        assertFalse(rendered.contains("smtp-password"));
        assertEquals(NotifyOutboxWakeSignal.TYPE_WAKE, signal.type());
        assertEquals("type", signal.getClass().getRecordComponents()[0].getName());
        assertEquals("outboxId", signal.getClass().getRecordComponents()[1].getName());
        assertEquals(2, signal.getClass().getRecordComponents().length);
    }

    @Test
    void submitDoesNotPublishWakeUntilTransactionCommits() {
        RecordingTransport transport = new RecordingTransport();
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        UserService users = mock(UserService.class);
        DispatchNotificationService dispatch = mock(DispatchNotificationService.class);
        stubSubmitDao(dao, users);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(NotifyOutboxWakePublisher.class,
                () -> new NotifyOutboxWakePublisher(transport, 250));
            context.refresh();
            NotificationApplicationRuntimeService runtime = new NotificationApplicationRuntimeService(
                dao, users, dispatch, context);
            TransactionContext.bind("notify-outbox-wake-submit");
            try {
                runtime.submit(submitCommand());
                verify(dao).insert(argThat((NotifyOutbox outbox) ->
                    outbox.getAvailableAt().equals(java.time.LocalDateTime.of(2026, 9, 19, 0, 0))
                        && outbox.getNextAttemptAt().equals(outbox.getAvailableAt())));
                assertTrue(transport.published.isEmpty());
                TransactionContext.getSynchronizations().getFirst().afterCommit();
                assertEquals(1, transport.published.size());
                assertEquals(NotifyOutboxWakeChannels.REDIS_CHANNEL, transport.published.getFirst().channel());
                String rendered = String.valueOf(transport.published.getFirst().signal());
                assertFalse(rendered.contains("13800138000"));
                assertFalse(rendered.contains("secret-user@example.com"));
                assertFalse(rendered.contains("通知正文"));
            } finally {
                TransactionContext.removeSynchronizations();
                TransactionContext.remove();
            }
        }
    }

    @Test
    void retryDoesNotPublishWakeUntilTransactionCommits() {
        RecordingTransport transport = new RecordingTransport();
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        UserService users = mock(UserService.class);
        DispatchNotificationService dispatch = mock(DispatchNotificationService.class);
        stubRetryDao(dao);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(NotifyOutboxWakePublisher.class,
                () -> new NotifyOutboxWakePublisher(transport, 250));
            context.refresh();
            NotificationApplicationRuntimeService runtime = new NotificationApplicationRuntimeService(
                dao, users, dispatch, context);
            TransactionContext.bind("notify-outbox-wake-retry");
            try {
                runtime.retry(new NotificationRetryCommand("9", null, "manual", null));
                verify(dao).requeueOutbox(argThat((NotifyOutbox outbox) -> outbox.getOutboxId() == 18L),
                    eq(java.time.LocalDateTime.of(2026, 9, 19, 0, 0)));
                assertTrue(transport.published.isEmpty());
                TransactionContext.getSynchronizations().getFirst().afterCommit();
                assertEquals(1, transport.published.size());
            } finally {
                TransactionContext.removeSynchronizations();
                TransactionContext.remove();
            }
        }
    }

    @Test
    void cancelDoesNotRequestWake() {
        RecordingTransport transport = new RecordingTransport();
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        UserService users = mock(UserService.class);
        DispatchNotificationService dispatch = mock(DispatchNotificationService.class);
        NotifyIntent intent = new NotifyIntent();
        intent.setIntentId(9L);
        intent.setStatus("QUEUED");
        when(dao.lockIntent(9L)).thenReturn(intent);
        when(dao.update(any(NotifyIntent.class))).thenReturn(1);
        when(dao.updateDeliveryStatus(9L, "PENDING", "CANCELLED")).thenReturn(1);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(NotifyOutboxWakePublisher.class,
                () -> new NotifyOutboxWakePublisher(transport, 250));
            context.refresh();
            NotificationApplicationRuntimeService runtime = new NotificationApplicationRuntimeService(
                dao, users, dispatch, context);
            TransactionContext.bind("notify-outbox-wake-cancel");
            try {
                runtime.cancel(new NotificationCancelCommand("9", "user-cancel"));
                assertTrue(TransactionContext.getSynchronizations() == null
                    || TransactionContext.getSynchronizations().isEmpty());
                assertTrue(transport.published.isEmpty());
            } finally {
                TransactionContext.removeSynchronizations();
                TransactionContext.remove();
            }
        }
        verify(dao, never()).insert(any(NotifyOutbox.class));
    }

    private static void stubSubmitDao(NotifyNotificationDao dao, UserService users) {
        when(dao.databaseNow()).thenReturn(java.time.LocalDateTime.of(2026, 9, 19, 0, 0, 0, 900_000_000));
        UserDTO user = new UserDTO();
        user.setUserId(101L);
        user.setPhoneNumber("13800138000");
        user.setEmail("secret-user@example.com");
        when(users.selectNotificationUsers(any())).thenReturn(List.of(user));
        when(dao.intentByIdempotency(anyString(), anyString())).thenReturn(null);
        when(dao.insert(any(NotifyIntent.class))).thenReturn(1);
        when(dao.insert(any(NotifyRecipient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dao.insert(any(NotifyDelivery.class))).thenReturn(1);
        when(dao.insert(any(NotifyOutbox.class))).thenReturn(1);
        when(dao.intent(anyLong())).thenAnswer(invocation -> {
            NotifyIntent intent = new NotifyIntent();
            intent.setIntentId(invocation.getArgument(0));
            intent.setMode("ASYNC");
            intent.setStatus("QUEUED");
            return intent;
        });
        when(dao.deliveries(anyLong())).thenAnswer(invocation -> {
            NotifyDelivery delivery = new NotifyDelivery();
            delivery.setDeliveryId(2L);
            delivery.setUserId(101L);
            delivery.setChannel("IN_APP");
            delivery.setStatus("PENDING");
            return List.of(delivery);
        });
    }

    private static void stubRetryDao(NotifyNotificationDao dao) {
        when(dao.databaseNow()).thenReturn(java.time.LocalDateTime.of(2026, 9, 19, 0, 0, 0, 900_000_000));
        NotifyIntent intent = new NotifyIntent();
        intent.setIntentId(9L);
        intent.setStatus("FAILED");
        intent.setStrategy("ALL");
        intent.setMode("ASYNC");
        intent.setPriority(0);
        when(dao.lockIntent(9L)).thenReturn(intent);
        NotifyDelivery delivery = new NotifyDelivery();
        delivery.setDeliveryId(8L);
        delivery.setIntentId(9L);
        delivery.setChannel("SMS");
        delivery.setStatus("FAILED");
        delivery.setErrorCode("UNBOUND_CHANNEL");
        when(dao.deliveries(9L)).thenReturn(List.of(delivery));
        when(dao.lockDeliveries(9L)).thenReturn(List.of(delivery));
        NotifyOutbox outbox = new NotifyOutbox();
        outbox.setOutboxId(18L);
        outbox.setIntentId(9L);
        outbox.setDeliveryId(8L);
        outbox.setStatus("DONE");
        outbox.setLastErrorCode("UNBOUND_CHANNEL");
        outbox.setAttemptCount(1);
        outbox.setMaxAttempts(5);
        when(dao.lockOutboxes(9L, List.of(8L))).thenReturn(List.of(outbox));
        when(dao.markDeliveryForRetry(9L, 8L, "FAILED", "UNBOUND_CHANNEL")).thenReturn(1);
        when(dao.requeueOutbox(eq(outbox), any())).thenReturn(1);
        when(dao.update(any(NotifyIntent.class))).thenReturn(1);
    }

    private static NotificationCommand submitCommand() {
        return new NotificationCommand(
            "profile", "person-rebind", "PERSON_REBIND", "biz-1",
            "USER", List.of("101"), "person-rebind",
            Map.of("title", "secret-title", "content", "通知正文"),
            List.of(NotificationChannel.IN_APP),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0,
            null, null, "key-1", Map.of("token", "smtp-password"), java.util.List.of());
    }

    private static final class RecordingTransport implements NotifyOutboxWakeTransport {
        private final List<PublishedWake> published = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public java.util.concurrent.Future<Long> publish(String channel, NotifyOutboxWakeSignal signal) {
            if (failure != null) {
                throw failure;
            }
            published.add(new PublishedWake(channel, signal));
            return java.util.concurrent.CompletableFuture.completedFuture(1L);
        }
    }

    private record PublishedWake(String channel, NotifyOutboxWakeSignal signal) {
    }
}
