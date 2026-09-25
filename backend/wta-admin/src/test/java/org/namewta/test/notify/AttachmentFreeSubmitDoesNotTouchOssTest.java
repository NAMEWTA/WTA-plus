package org.namewta.test.notify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.system.api.OssService;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 无附件提交必须走真实 Runtime.submit，且不调用 OSS。 */
@Tag("dev")
class AttachmentFreeSubmitDoesNotTouchOssTest {

    @Test
    void inAppSubmitWithNoAttachmentsDoesNotCallOss() throws Exception {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        UserService users = mock(UserService.class);
        DispatchNotificationService dispatch = mock(DispatchNotificationService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        OssService oss = mock(OssService.class);
        UserDTO user = new UserDTO();
        user.setUserId(7L);
        user.setEmail("person@example.com");
        when(users.selectNotificationUsers(anyList())).thenReturn(List.of(user));
        when(dao.insert(any(NotifyIntent.class))).thenReturn(1);
        when(dao.databaseNow()).thenReturn(LocalDateTime.parse("2026-09-24T00:00:00"));
        when(dao.intent(any())).thenAnswer(invocation -> {
            NotifyIntent stored = new NotifyIntent();
            stored.setIntentId(invocation.getArgument(0));
            stored.setStatus("QUEUED");
            return stored;
        });
        when(dao.deliveries(any())).thenReturn(List.of());
        NotificationApplicationRuntimeService runtime =
            new NotificationApplicationRuntimeService(dao, users, dispatch, events);
        Field ossField = NotificationApplicationRuntimeService.class.getDeclaredField("attachmentOssService");
        ossField.setAccessible(true);
        ossField.set(runtime, oss);

        runtime.submit(new NotificationCommand(
            "profile", "person-rebind", "PERSON_REBIND", "7",
            "USER", List.of("7"), "person-rebind",
            Map.of("title", "hello", "content", "body"),
            List.of(NotificationChannel.IN_APP), NotificationStrategy.ALL, NotificationMode.ASYNC,
            0, null, null, "profile:person-rebind:7", Map.of(), List.of()));

        verify(dao).insertFanout(anyList(), anyList(), anyList());
        verifyNoInteractions(oss);
    }
}
