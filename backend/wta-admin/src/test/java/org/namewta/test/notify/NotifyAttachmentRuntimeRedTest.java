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
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.service.runtime.DispatchNotificationService;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 当前公开命令的附件参数经过持久模板快照后，必须到达真正渠道请求。 */
@Tag("dev")
class NotifyAttachmentRuntimeRedTest {

    @Test
    @SuppressWarnings("unchecked")
    void persistedMailAttachmentIdsReachNotifyRequestInOriginalOrder() {
        NotificationCommand submitted = new NotificationCommand("demo", "person-rebind", "demo_mail", "owned-1",
            "EMAIL", List.of("user@example.com"), "person-rebind",
            Map.of("title", "主题", "content", "正文", "attachmentOssIds", List.of(77L, 88L)),
            List.of(NotificationChannel.MAIL), NotificationStrategy.ALL, NotificationMode.ASYNC,
            0, null, null, "owned-attachment-red", Map.of());
        NotifyIntent persisted = new NotifyIntent();
        persisted.setIntentId(1L);
        persisted.setSceneCode(submitted.sceneCode());
        persisted.setTemplateCode(submitted.templateCode());
        persisted.setBizType(submitted.bizType());
        persisted.setBizId(submitted.bizId());
        persisted.setStrategy(submitted.strategy().name());
        persisted.setTemplateParamsJson(JsonUtils.toJsonString(submitted.templateParams()));

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
    }
}
