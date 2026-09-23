package org.namewta.notify.service.runtime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyMessage;
import org.namewta.notify.domain.entity.NotifyMessageRecipient;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** DAO 零行属于持久化冲突，不能发布提交后提示或继续写下一行。 */
@Tag("dev")
class InAppNotificationServiceTest {
    private final NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final InAppNotificationService service = new InAppNotificationService(dao, events);
    private final InAppNotificationPort.InAppSnapshot snapshot =
        new InAppNotificationPort.InAppSnapshot("title", "body", "/notify/notice", "", List.of("IN_APP"));

    @Test
    void messageInsertZeroStopsBeforeRecipientAndEvent() {
        when(dao.insert(any(NotifyMessage.class))).thenReturn(0);

        assertThatThrownBy(() -> service.persist("1", snapshot, List.of(7L)))
            .isInstanceOf(IllegalStateException.class);
        verify(dao, never()).insert(any(NotifyMessageRecipient.class));
        verifyNoInteractions(events);
    }

    @Test
    void recipientInsertZeroStopsBeforeEvent() {
        when(dao.insert(any(NotifyMessage.class))).thenReturn(1);
        when(dao.insert(any(NotifyMessageRecipient.class))).thenReturn(0);

        assertThatThrownBy(() -> service.persist("1", snapshot, List.of(7L)))
            .isInstanceOf(IllegalStateException.class);
        verify(events, never()).publishEvent(any(Object.class));
    }
}
