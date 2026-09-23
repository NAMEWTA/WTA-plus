package org.namewta.notify.service;

import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.dao.NotifyPersistenceDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyNotice;
import org.namewta.notify.domain.entity.NotifyNoticeSnapshot;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

        assertThrows(ServiceException.class, () -> new NotifyNoticePublisherService(app, dao, users,
            mock(NotifyNotificationDao.class)).publish(notice));
        verify(dao, never()).insertSnapshot(any());
        verify(app, never()).submit(any());
    }

    @Test
    void userAudienceUsesAuthorizedUsersAndAllKeepsEmptyRecipientIds() {
        var app = mock(NotificationApplicationService.class);
        var dao = mock(NotifyPersistenceDao.class);
        var notifications = mock(NotifyNotificationDao.class);
        var users = mock(UserService.class);
        var user = new UserDTO();
        user.setUserId(42L);
        when(users.selectNotificationUsers(List.of(42L))).thenReturn(List.of(user));
        when(dao.latestSnapshot(1L)).thenReturn(null);
        when(dao.insertSnapshot(any())).thenReturn(1);
        when(dao.updateSnapshotPath(any(), eq("/notify/inbox?messageId=123"))).thenReturn(1);
        when(notifications.updateNoticePath(any(), anyString(), eq("/notify/inbox?messageId=123"))).thenReturn(1);
        var submitted = new java.util.concurrent.atomic.AtomicReference<NotificationCommand>();
        when(app.submit(any())).thenAnswer(invocation -> {
            submitted.set(invocation.getArgument(0));
            return new NotificationReceipt("123", NotificationStatus.QUEUED, true, false, List.of());
        });
        when(notifications.lockNoticeIntent("notice-published:1:1")).thenAnswer(ignored -> {
            NotificationCommand command = submitted.get();
            NotifyIntent intent = new NotifyIntent();
            intent.setIntentId(123L);
            intent.setAppId(command.appId());
            intent.setSceneCode(command.sceneCode());
            intent.setTemplateCode(command.templateCode());
            intent.setBizType(command.bizType());
            intent.setBizId(command.bizId());
            intent.setIdempotencyKey(command.idempotencyKey());
            intent.setPathSnapshot("/notify/inbox");
            intent.setTemplateParamsJson(JsonUtils.toJsonString(command.templateParams()));
            intent.setMetadataJson(JsonUtils.toJsonString(command.metadata()));
            return intent;
        });
        NotifyNoticePublisherService publisher = new NotifyNoticePublisherService(app, dao, users, notifications);

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
        assertEquals(List.of("/notify/inbox", "/notify/inbox"), commands.getAllValues().stream()
            .map(command -> command.templateParams().get("path")).toList());
        verify(notifications).updateNoticePath(any(), argThat(json -> json.contains("/notify/inbox?messageId=123")),
            eq("/notify/inbox?messageId=123"));
        verify(users).selectNotificationUsers(List.of(42L));
    }

    @Test
    void externalOnlyDuplicateWithAnotherSnapshotCannotBeAcceptedAsNewPublication() {
        var app = mock(NotificationApplicationService.class);
        var dao = mock(NotifyPersistenceDao.class);
        var users = mock(UserService.class);
        var notifications = mock(NotifyNotificationDao.class);
        var user = new UserDTO();
        user.setUserId(42L);
        when(users.selectNotificationUsers(List.of(42L))).thenReturn(List.of(user));
        when(dao.insertSnapshot(any())).thenReturn(1);
        when(app.submit(any())).thenReturn(new NotificationReceipt("123", NotificationStatus.QUEUED, true, false, List.of()));
        NotifyIntent old = new NotifyIntent();
        old.setIntentId(123L);
        old.setAppId("notify");
        old.setSceneCode("notice-published");
        old.setTemplateCode("notice-published");
        old.setBizType("NOTICE_PUBLISHED");
        old.setBizId("1");
        old.setIdempotencyKey("notice-published:1:1");
        old.setMetadataJson(JsonUtils.toJsonString(Map.of("audit", "NOTICE_SNAPSHOT", "noticeVersion",
            Map.of("noticeId", 1L, "snapshotId", 98L, "version", 1, "retracted", false))));
        when(notifications.lockNoticeIntent("notice-published:1:1")).thenReturn(old);

        try (var ids = mockStatic(IdGeneratorUtil.class)) {
            ids.when(IdGeneratorUtil::nextLongId).thenReturn(99L);
            assertThrows(ServiceException.class, () -> new NotifyNoticePublisherService(app, dao, users,
                notifications).publish(notice("USER", "[42]", "[]", "[\"MAIL\"]")));
        }
        verify(notifications, never()).updateNoticePath(any(), anyString(), anyString());
        verify(dao, never()).updateSnapshotPath(any(), anyString());
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
