package org.namewta.notify.service.runtime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.api.NotificationRetryCommand;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.system.api.UserService;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 不支持的通知编排必须在幂等查询、收件人解析和持久化之前拒绝。 */
@Tag("dev")
class NotificationSupportedModeRuntimeTest {
    @Test
    void supportedDuplicateStillReturnsTheOriginalReceipt() {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        DispatchNotificationService dispatch = mock(DispatchNotificationService.class);
        var service = new NotificationApplicationRuntimeService(dao, mock(UserService.class), dispatch,
            mock(ApplicationEventPublisher.class));
        NotifyIntent duplicate = new NotifyIntent();
        duplicate.setIntentId(42L);
        duplicate.setStatus("QUEUED");
        when(dao.intentByIdempotency("owned-app", "existing-request")).thenReturn(duplicate);
        when(dao.deliveries(42L)).thenReturn(List.of());
        NotificationCommand command = command(NotificationStrategy.ALL, 0);

        assertEquals("42", service.submit(command).notificationId());
        verify(dispatch, never()).validateSubmission(any());
        verify(dao, never()).insert(any(NotifyIntent.class));
    }

    @Test
    void retryRejectsUnsupportedHistoricalIntentBeforeCandidateScan() {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        UserService users = mock(UserService.class);
        DispatchNotificationService dispatch = mock(DispatchNotificationService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        var service = new NotificationApplicationRuntimeService(dao, users, dispatch, events);
        NotifyIntent old = new NotifyIntent();
        old.setIntentId(42L);
        old.setStrategy("ESCALATION");
        old.setMode("ASYNC");
        old.setPriority(0);
        when(dao.lockIntent(42L)).thenReturn(old);
        NotifyDelivery target = new NotifyDelivery();
        target.setDeliveryId(43L);
        target.setIntentId(42L);
        when(dao.delivery(43L)).thenReturn(target);

        assertThrows(ServiceException.class,
            () -> service.retry(new NotificationRetryCommand("42", "43", "owned retry", null)));
        verify(dao, never()).lockOutboxes(any(), any());
        verify(dao, never()).requeueOutbox(any(), any());
        verifyNoInteractions(users, dispatch, events);
    }

    @ParameterizedTest(name = "strategy={0}, priority={1}")
    @MethodSource("unsupportedCommands")
    void unsupportedSubmissionFailsBeforeDuplicateLookup(NotificationStrategy strategy, int priority) {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        UserService users = mock(UserService.class);
        DispatchNotificationService dispatch = mock(DispatchNotificationService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        NotificationApplicationRuntimeService service =
            new NotificationApplicationRuntimeService(dao, users, dispatch, events);

        // 旧实现若先查重会返回这张合法回执，红灯应来自缺失的模式拒绝，而非空夹具异常。
        NotifyIntent duplicate = new NotifyIntent();
        duplicate.setIntentId(42L);
        duplicate.setStatus("QUEUED");
        when(dao.intentByIdempotency("owned-app", "existing-request")).thenReturn(duplicate);
        when(dao.deliveries(42L)).thenReturn(List.of());
        NotificationCommand command = command(strategy, priority);

        assertThrows(ServiceException.class, () -> service.submit(command));
        verifyNoInteractions(dao, users, dispatch, events);
    }

    private static NotificationCommand command(NotificationStrategy strategy, int priority) {
        return new NotificationCommand("owned-app", "owned-scene", "OWNED", "owned-biz",
            "EMAIL", List.of("recipient@example.test"), "owned-template", Map.of(),
            List.of(NotificationChannel.MAIL), strategy, NotificationMode.ASYNC, priority,
            null, null, "existing-request", Map.of(), java.util.List.of());
    }

    private static Stream<Arguments> unsupportedCommands() {
        return Stream.of(
            Arguments.of(NotificationStrategy.ORDERED_FALLBACK, 0),
            Arguments.of(NotificationStrategy.ESCALATION, 0),
            Arguments.of(NotificationStrategy.ALL, 1),
            Arguments.of(NotificationStrategy.ALL, -1),
            Arguments.of(NotificationStrategy.ALL, 80));
    }
}
