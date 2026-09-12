package org.namewta.notify.service.runtime;

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
                () -> new NotifyOutboxWakePublisher(transport, event -> { }));
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
                () -> new NotifyOutboxWakePublisher(transport, event -> { }));
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
                () -> new NotifyOutboxWakePublisher(transport, event -> { }));
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
                () -> new NotifyOutboxWakePublisher(transport, event -> { }));
            context.refresh();
            NotificationApplicationRuntimeService runtime = new NotificationApplicationRuntimeService(
                dao, users, dispatch, context);
            TransactionContext.bind("notify-outbox-wake-submit");
            try {
                runtime.submit(submitCommand());
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
                () -> new NotifyOutboxWakePublisher(transport, event -> { }));
            context.refresh();
            NotificationApplicationRuntimeService runtime = new NotificationApplicationRuntimeService(
                dao, users, dispatch, context);
            TransactionContext.bind("notify-outbox-wake-retry");
            try {
                runtime.retry(new NotificationRetryCommand("9", null, "manual", "retry-1"));
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
        when(dao.intent(9L)).thenReturn(intent);
        when(dao.update(any(NotifyIntent.class))).thenReturn(1);
        when(dao.updateDeliveryStatus(9L, "PENDING", "CANCELLED")).thenReturn(1);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DsTxEventListenerFactory.class);
            context.registerBean(NotifyOutboxWakePublisher.class,
                () -> new NotifyOutboxWakePublisher(transport, event -> { }));
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
        NotifyIntent intent = new NotifyIntent();
        intent.setIntentId(9L);
        intent.setStatus("FAILED");
        when(dao.intent(9L)).thenReturn(intent);
        NotifyDelivery delivery = new NotifyDelivery();
        delivery.setDeliveryId(8L);
        delivery.setStatus("FAILED");
        when(dao.deliveries(9L)).thenReturn(List.of(delivery));
        when(dao.markDeliveryForRetry(8L)).thenReturn(1);
        when(dao.requeueOutbox(eq(8L), any())).thenReturn(1);
        when(dao.update(any(NotifyIntent.class))).thenReturn(1);
    }

    private static NotificationCommand submitCommand() {
        return new NotificationCommand(
            "profile", "person-rebind", "PERSON_REBIND", "biz-1",
            "USER", List.of("101"), "person-rebind",
            Map.of("title", "secret-title", "content", "通知正文"),
            List.of(NotificationChannel.IN_APP),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0,
            null, null, "key-1", Map.of("token", "smtp-password"));
    }

    private static final class RecordingTransport implements NotifyOutboxWakeTransport {
        private final List<PublishedWake> published = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public void publish(String channel, NotifyOutboxWakeSignal signal) {
            if (failure != null) {
                throw failure;
            }
            published.add(new PublishedWake(channel, signal));
        }
    }

    private record PublishedWake(String channel, NotifyOutboxWakeSignal signal) {
    }
}
