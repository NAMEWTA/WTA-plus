package org.namewta.notify.service;

import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.notify.domain.entity.NotifyNoticeSnapshot;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** 公告发布时目标解析、渠道快照和失败原子性回归测试。 */
@Tag("dev")
class NotifyNoticePublisherServiceTest {
    @Test
    void userTypeWithoutResolvedUsersFailsBeforeSnapshotOrSubmit() {
        var app = mock(NotificationApplicationService.class);
        var dao = mock(NotifyPersistenceDao.class);
        var users = mock(UserService.class);
        when(users.selectUsersByUserTypeIds(List.of(8L))).thenReturn(List.of());
        NotifyNotice notice = notice("USER_TYPE", "[]", "[8]", "[\"IN_APP\"]");

        assertThrows(ServiceException.class, () -> new NotifyNoticePublisherService(app, dao, users).publish(notice));
        verify(dao, never()).insertSnapshot(any());
        verify(app, never()).submit(any());
    }

    @Test
    void userAudienceUsesAuthorizedUsersAndAllKeepsEmptyRecipientIds() {
        var app = mock(NotificationApplicationService.class);
        var dao = mock(NotifyPersistenceDao.class);
        var users = mock(UserService.class);
        var user = new UserDTO();
        user.setUserId(42L);
        when(users.selectNotificationUsers(List.of(42L))).thenReturn(List.of(user));
        when(dao.latestSnapshot(1L)).thenReturn(null);
        NotifyNoticePublisherService publisher = new NotifyNoticePublisherService(app, dao, users);

        try (var ids = mockStatic(IdGeneratorUtil.class)) {
            ids.when(IdGeneratorUtil::nextLongId).thenReturn(99L);
            publisher.publish(notice("USER", "[42]", "[]", "[\"SMS\"]"));
            publisher.publish(notice("ALL", "[]", "[]", "[\"IN_APP\"]"));
        }

        ArgumentCaptor<NotificationCommand> commands = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(app, times(2)).submit(commands.capture());
        assertEquals(List.of(0, 0), commands.getAllValues().stream().map(NotificationCommand::priority).toList());
        assertEquals(List.of("42"), commands.getAllValues().get(0).recipientIds());
        assertEquals(List.of(), commands.getAllValues().get(1).recipientIds());
        assertEquals(List.of(org.namewta.notify.api.NotificationChannel.SMS), commands.getAllValues().get(0).channels());
        assertEquals(List.of(org.namewta.notify.api.NotificationChannel.IN_APP), commands.getAllValues().get(1).channels());
        verify(users).selectNotificationUsers(List.of(42L));
    }

    private static NotifyNotice notice(String type, String recipientIds, String userTypes, String channels) {
        NotifyNotice notice = new NotifyNotice();
        notice.setNoticeId(1L);
        notice.setNoticeTitle("title");
        notice.setNoticeContent("content");
        notice.setNoticeType("1");
        notice.setRecipientType(type);
        notice.setRecipientIdsJson(recipientIds);
        notice.setUserTypeIdsJson(userTypes);
        notice.setChannelsJson(channels);
        notice.setPublishedAt(LocalDateTime.now());
        return notice;
    }
}
