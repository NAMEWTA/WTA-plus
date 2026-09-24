package org.namewta.test.notify;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.NotifyRequest;
import org.namewta.common.notify.model.NotifyResult;
import org.namewta.common.notify.model.NotifyStatus;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyIntentAttachment;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.namewta.notify.port.NotifyAttachmentActorPort;
import org.namewta.system.api.OssService;
import org.namewta.system.api.UserService;
import org.namewta.common.core.exception.ServiceException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 当前公开命令的附件参数经过持久模板快照后，必须到达真正渠道请求。 */
@Tag("dev")
class NotifyAttachmentRuntimeRedTest {

    @Test
    void quotedSnowflakeAttachmentIdRetainsPrecisionAndInvalidIdsFailBeforeWrites() {
        String exact = "9007199254740993";
        NotificationCommand encoded = command(List.of(exact));
        NotificationCommand decoded = JsonUtils.parseObject(JsonUtils.toJsonString(encoded), NotificationCommand.class);
        assertThat(decoded.attachmentOssIds()).containsExactly(exact);

        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        NotificationApplicationRuntimeService runtime = new NotificationApplicationRuntimeService(
            dao, mock(UserService.class), mock(DispatchNotificationService.class), event -> { });
        for (String invalid : List.of("0", "-1", "1.5", "9223372036854775808", "01", "+1")) {
            assertThatThrownBy(() -> runtime.submit(command(List.of(invalid))))
                .as("无效附件ID在任何持久写入前拒绝: %s", invalid)
                .isInstanceOf(ServiceException.class);
        }
        verify(dao, never()).insert(any(NotifyIntent.class));

        NotifyAttachmentActorPort actor = () -> new NotifyAttachmentActorPort.Actor(1L, 2L);
        ReflectionTestUtils.setField(runtime, "attachmentActorPort", actor);
        ReflectionTestUtils.setField(runtime, "attachmentOssService", mock(OssService.class));
        assertThatThrownBy(() -> runtime.submit(decoded)).isInstanceOf(IllegalStateException.class);
        ArgumentCaptor<NotifyIntentAttachment> relation = ArgumentCaptor.forClass(NotifyIntentAttachment.class);
        verify(dao).insert(relation.capture());
        assertThat(relation.getValue().getSourceOssId()).isEqualTo(9_007_199_254_740_993L);
    }

    private NotificationCommand command(List<String> ids) {
        return new NotificationCommand("demo", "demo-mail", "demo", "owned-decimal", "EMAIL",
            List.of("user@example.com"), "demo-mail", Map.of("title", "subject", "content", "body"),
            List.of(NotificationChannel.MAIL), NotificationStrategy.ALL, NotificationMode.ASYNC,
            0, null, null, null, Map.of(), ids);
    }

    @Test
    @SuppressWarnings("unchecked")
    void persistedMailAttachmentIdsReachNotifyRequestInOriginalOrder() {
        NotificationCommand submitted = new NotificationCommand("demo", "person-rebind", "demo_mail", "owned-1",
            "EMAIL", List.of("user@example.com"), "person-rebind",
            Map.of("title", "主题", "content", "正文"),
            List.of(NotificationChannel.MAIL), NotificationStrategy.ALL, NotificationMode.ASYNC,
            0, null, null, "owned-attachment-red", Map.of(), List.of("77", "88"));
        NotifyIntent persisted = new NotifyIntent();
        persisted.setIntentId(1L);
        persisted.setSceneCode(submitted.sceneCode());
        persisted.setTemplateCode(submitted.templateCode());
        persisted.setBizType(submitted.bizType());
        persisted.setBizId(submitted.bizId());
        persisted.setStrategy(submitted.strategy().name());
        persisted.setTemplateParamsJson(JsonUtils.toJsonString(submitted.templateParams()));
        persisted.setAttachmentActorUserId(91L);
        persisted.setAttachmentActorClientPk(92L);

        NotifyDelivery delivery = new NotifyDelivery();
        delivery.setDeliveryId(2L);
        delivery.setIntentId(1L);
        delivery.setChannel("MAIL");
        delivery.setTargetValue("user@example.com");
        delivery.setStatus("PENDING");
        NotifyOutbox lease = new NotifyOutbox();
        lease.setOutboxId(3L);
        lease.setIntentId(1L);
        lease.setDeliveryId(2L);
        lease.setStatus("PROCESSING");
        lease.setLeaseOwner("worker-owned");
        lease.setLeaseToken("lease-owned");
        lease.setLeaseUntil(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));

        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        when(dao.databaseNow()).thenReturn(LocalDateTime.now(ZoneOffset.UTC));
        when(dao.outbox(3L)).thenReturn(lease);
        when(dao.intent(1L)).thenReturn(persisted);
        NotifyIntentAttachment first = new NotifyIntentAttachment();
        first.setSourceOssId(77L);
        NotifyIntentAttachment second = new NotifyIntentAttachment();
        second.setSourceOssId(88L);
        when(dao.attachments(1L)).thenReturn(List.of(first, second));
        when(dao.delivery(2L)).thenReturn(delivery);
        NotifyConfigDao configDao = mock(NotifyConfigDao.class);
        NotifySceneBinding binding = new NotifySceneBinding();
        binding.setAccountId(11L);
        binding.setMailSubject("${title}");
        binding.setMailBody("${content}");
        when(configDao.findBinding("person-rebind", "MAIL")).thenReturn(binding);
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(11L);
        account.setChannel("MAIL");
        account.setConfigKey("owned-mail");
        account.setEnabled("Y");
        account.setMinuteMax(100);
        when(configDao.findAccount(11L)).thenReturn(account);
        NotifyDispatchResultPort resultPort = mock(NotifyDispatchResultPort.class);
        when(resultPort.renew(any())).thenReturn(true);
        when(resultPort.deadlineGate(any())).thenReturn(true);
        NotifyClient client = mock(NotifyClient.class);
        when(client.send(any())).thenReturn(new NotifyResult("2", org.namewta.common.notify.model.NotifyChannel.MAIL,
            "owned-mail", NotifyStatus.ACCEPTED, List.of()));
        DispatchNotificationService runtime = new DispatchNotificationService(dao, client,
            mock(ObjectProvider.class), configDao, (key, limit, duration) -> true, resultPort);

        runtime.dispatch(lease);

        ArgumentCaptor<NotifyRequest> request = ArgumentCaptor.forClass(NotifyRequest.class);
        verify(client).send(request.capture());
        assertEquals(List.of(77L, 88L), request.getValue().attachmentOssIds());
        assertEquals(1L, request.getValue().attachmentOwnerIntentId());
    }
}
