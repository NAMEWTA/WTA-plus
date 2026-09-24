package org.namewta.notify.service.runtime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** ALL 人数边界只验证解析和写入前置条件，不作为十万人持久化或性能证据。 */
@Tag("dev")
class NotificationAllRecipientLimitTest {
    @Test
    void exactlyOneHundredThousandReachesFirstIntentWrite() {
        Fixture fixture = new Fixture(100_000);
        doThrow(new FirstIntentWriteReached()).when(fixture.dao).insert(any(NotifyIntent.class));

        assertThrows(FirstIntentWriteReached.class, () -> fixture.runtime.submit(command()));

        fixture.assertPaging();
        verify(fixture.dao).insert(any(NotifyIntent.class));
        verifyNoMoreInteractions(fixture.dao);
        verifyNoInteractions(fixture.events);
    }

    @Test
    void oneMoreThanLimitFailsBeforeAnyBusinessWrite() {
        Fixture fixture = new Fixture(100_001);

        ServiceException failure = assertThrows(ServiceException.class,
            () -> fixture.runtime.submit(command()));

        assertEquals("全体用户通知超过单次发送上限", failure.getMessage());
        fixture.assertPaging();
        verifyNoInteractions(fixture.dao, fixture.events);
    }

    private static NotificationCommand command() {
        return new NotificationCommand("owned-limit", "owned-limit", "OWNED", "boundary",
            "ALL", List.of(), "owned-limit", Map.of("title", "limit", "content", "limit"),
            List.of(NotificationChannel.IN_APP), NotificationStrategy.ALL, NotificationMode.ASYNC,
            0, null, null, null, Map.of(), List.of());
    }

    private static final class Fixture {
        final NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        final UserService directory = mock(UserService.class);
        final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        final DispatchNotificationService dispatch = mock(DispatchNotificationService.class);
        final NotificationApplicationRuntimeService runtime =
            new NotificationApplicationRuntimeService(dao, directory, dispatch, events);
        final List<Integer> offsets = new ArrayList<>();

        Fixture(int count) {
            when(directory.selectAllActiveUsers(anyInt(), anyInt())).thenAnswer(invocation -> {
                int offset = invocation.getArgument(0);
                int limit = invocation.getArgument(1);
                assertEquals(1_000, limit);
                offsets.add(offset);
                List<UserDTO> users = new ArrayList<>();
                for (int index = offset; index < Math.min(offset + limit, count); index++) {
                    UserDTO user = new UserDTO();
                    user.setUserId((long) index + 1);
                    users.add(user);
                }
                return users;
            });
        }

        void assertPaging() {
            assertEquals(101, offsets.size());
            for (int page = 0; page < offsets.size(); page++) {
                assertEquals(page * 1_000, offsets.get(page));
            }
            verify(dispatch).validateSubmission(command());
            verifyNoMoreInteractions(dispatch);
        }
    }

    private static final class FirstIntentWriteReached extends RuntimeException { }
}
