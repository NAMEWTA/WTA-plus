package org.namewta.notify.service.runtime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.api.*;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.system.api.UserService;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 收件人/配置预处理跨过截止时，真正提交前须再次拒绝。 */
@Tag("dev")
class NotificationDeadlineRuntimeTest {
    @Test
    void slowPreparationCannotInsertAlreadyExpiredIntent() {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        LocalDateTime before = LocalDateTime.of(2026, 9, 23, 12, 0, 0);
        when(dao.databaseNow()).thenReturn(before, before.plusMinutes(3));
        var service = new NotificationApplicationRuntimeService(dao, mock(UserService.class),
            mock(DispatchNotificationService.class), mock(ApplicationEventPublisher.class));
        Instant deadline = before.plusMinutes(2).toInstant(ZoneOffset.UTC);
        var command = new NotificationCommand("owned", "owned", "owned", "owned", "EMAIL",
            List.of("synthetic@example.test"), "owned", Map.of(), List.of(NotificationChannel.MAIL),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 0, null, deadline, "owned-deadline", Map.of(), java.util.List.of());

        assertThrows(ServiceException.class, () -> service.submit(command));
        verify(dao, never()).insert(any(org.namewta.notify.domain.entity.NotifyIntent.class));
    }
}
