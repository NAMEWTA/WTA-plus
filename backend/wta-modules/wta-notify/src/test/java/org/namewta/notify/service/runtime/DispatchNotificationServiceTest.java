package org.namewta.notify.service.runtime;

import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.model.NotifyChannel;
import org.namewta.common.notify.model.NotifyRequest;
import org.namewta.common.notify.model.NotifyResult;
import org.namewta.common.notify.model.NotifyRichContent;
import org.namewta.common.notify.model.NotifyStatus;
import org.namewta.common.notify.model.NotifyTemplateContent;
import org.namewta.notify.api.InAppNotificationPort;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.port.NotifyQuotaPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 投递按场景绑定账号，缺绑定、停用和超限失败关闭且不调用供应商。
 */
@Tag("dev")
class DispatchNotificationServiceTest {

    @Test
    void unboundMailFailsClosedWithoutProviderSend() {
        Fixture fixture = fixture("MAIL");
        when(fixture.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(null);

        fixture.service.dispatch(fixture.outbox);

        verify(fixture.notifyClient, never()).send(any());
        assertEquals("FAILED", fixture.delivery.getStatus());
        assertEquals("UNBOUND_CHANNEL", fixture.delivery.getErrorCode());
        assertEquals("DONE", fixture.outbox.getStatus());
    }

    @Test
    void unboundSmsFailsClosedWithoutProviderSend() {
        Fixture fixture = fixture("SMS");
        when(fixture.configDao.findBinding("auth-captcha", "SMS")).thenReturn(null);

        fixture.service.dispatch(fixture.outbox);

        verify(fixture.notifyClient, never()).send(any());
        assertEquals("FAILED", fixture.delivery.getStatus());
        assertEquals("UNBOUND_CHANNEL", fixture.delivery.getErrorCode());
        assertEquals("DONE", fixture.outbox.getStatus());
    }

    @Test
    void disabledAccountFailsClosedWithoutProviderSend() {
        Fixture fixture = fixture("MAIL");
        NotifySceneBinding binding = binding(11L, "MAIL", "${code}", "code=${code}");
        NotifyChannelAccount account = mailAccount(11L, "smtp-a", "N");
        when(fixture.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(binding);
        when(fixture.configDao.findAccount(11L)).thenReturn(account);

        fixture.service.dispatch(fixture.outbox);

        verify(fixture.notifyClient, never()).send(any());
        assertEquals("ACCOUNT_DISABLED", fixture.delivery.getErrorCode());
        assertEquals("DONE", fixture.outbox.getStatus());
    }

    @Test
    void boundMailUsesRenderedTemplateNotCallerSnapshots() {
        Fixture fixture = fixture("MAIL");
        fixture.intent.setTitleSnapshot("caller-title");
        fixture.intent.setContentSnapshot("caller-content-with-secret-sentence");
        NotifySceneBinding binding = binding(11L, "MAIL", "验证码 ${code}", "有效 ${expireMinutes} 分钟，码 ${code}");
        when(fixture.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(binding);
        when(fixture.configDao.findAccount(11L)).thenReturn(mailAccount(11L, "smtp-main", "Y"));
        when(fixture.notifyClient.send(any())).thenReturn(accepted("smtp-main", NotifyChannel.MAIL));

        fixture.service.dispatch(fixture.outbox);

        ArgumentCaptor<NotifyRequest> captor = ArgumentCaptor.forClass(NotifyRequest.class);
        verify(fixture.notifyClient).send(captor.capture());
        NotifyRequest request = captor.getValue();
        assertEquals("smtp-main", request.providerKey());
        assertEquals("2", request.requestId());
        assertEquals("2", request.idempotencyKey());
        NotifyRichContent content = assertInstanceOf(NotifyRichContent.class, request.content());
        assertEquals("验证码 1234", content.subject());
        assertEquals("有效 5 分钟，码 1234", content.content());
        assertEquals("ACCEPTED", fixture.delivery.getStatus());
    }

    @Test
    void boundSmsUsesVendorTemplateOnBoundAccountOnly() {
        Fixture fixture = fixture("SMS");
        NotifySceneBinding binding = binding(22L, "SMS", null, null);
        binding.setSmsTemplateCode("SMS_BOUND");
        binding.setSmsParamMappingJson(JsonUtils.toJsonString(Map.of("code", "code", "expireMinutes", "min")));
        NotifyChannelAccount bound = smsAccount(22L, "ali-prod");
        NotifyChannelAccount other = smsAccount(21L, "ali-other");
        other.setEnabled("Y");
        when(fixture.configDao.findBinding("auth-captcha", "SMS")).thenReturn(binding);
        when(fixture.configDao.findAccount(22L)).thenReturn(bound);
        when(fixture.notifyClient.send(any())).thenReturn(accepted("ali-prod", NotifyChannel.SMS));

        fixture.service.dispatch(fixture.outbox);

        ArgumentCaptor<NotifyRequest> captor = ArgumentCaptor.forClass(NotifyRequest.class);
        verify(fixture.notifyClient).send(captor.capture());
        assertEquals("ali-prod", captor.getValue().providerKey());
        NotifyTemplateContent content = assertInstanceOf(NotifyTemplateContent.class, captor.getValue().content());
        assertEquals("SMS_BOUND", content.providerTemplateCode());
        assertEquals("1234", content.params().get("code"));
        assertEquals("5", content.params().get("min"));
        verify(fixture.configDao, never()).findAccount(21L);
    }

    @Test
    void secondSendWithinAccountMinuteCapFailsClosed() {
        MemoryQuota quota = new MemoryQuota();
        Fixture first = fixture("MAIL", quota);
        Fixture second = fixture("MAIL", quota);
        NotifySceneBinding binding = binding(11L, "MAIL", "${code}", "${expireMinutes}");
        NotifyChannelAccount account = mailAccount(11L, "smtp-main", "Y");
        account.setMinuteMax(1);
        when(first.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(binding);
        when(first.configDao.findAccount(11L)).thenReturn(account);
        when(second.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(binding);
        when(second.configDao.findAccount(11L)).thenReturn(account);
        when(first.notifyClient.send(any())).thenReturn(accepted("smtp-main", NotifyChannel.MAIL));

        first.service.dispatch(first.outbox);
        second.service.dispatch(second.outbox);

        verify(second.notifyClient, never()).send(any());
        assertEquals("ACCOUNT_QUOTA", second.delivery.getErrorCode());
        assertEquals("DONE", second.outbox.getStatus());
    }

    @Test
    void recipientMinuteCapIsIsolatedByScene() {
        MemoryQuota quota = new MemoryQuota();
        Fixture first = fixture("MAIL", quota);
        Fixture second = fixture("MAIL", quota);
        Fixture otherScene = fixture("MAIL", quota);
        otherScene.intent.setSceneCode("notice-published");
        otherScene.intent.setTemplateCode("notice-published");
        otherScene.intent.setTemplateParamsJson(JsonUtils.toJsonString(
            Map.of("title", "公告标题", "content", "公告正文", "path", "/notify/notice")));
        NotifySceneBinding captcha = binding(11L, "MAIL", "${code}", "${expireMinutes}");
        captcha.setRestricted("Y");
        captcha.setRecipientMinuteMax(1);
        captcha.setRecipientDayMax(0);
        NotifySceneBinding notice = binding(11L, "MAIL", "${title}", "${content}${path}");
        notice.setSceneCode("notice-published");
        notice.setRestricted("Y");
        notice.setRecipientMinuteMax(1);
        notice.setRecipientDayMax(0);
        NotifyChannelAccount account = mailAccount(11L, "smtp-main", "Y");
        when(first.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(captcha);
        when(first.configDao.findAccount(11L)).thenReturn(account);
        when(second.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(captcha);
        when(second.configDao.findAccount(11L)).thenReturn(account);
        when(otherScene.configDao.findBinding("notice-published", "MAIL")).thenReturn(notice);
        when(otherScene.configDao.findAccount(11L)).thenReturn(account);
        when(first.notifyClient.send(any())).thenReturn(accepted("smtp-main", NotifyChannel.MAIL));
        when(otherScene.notifyClient.send(any())).thenReturn(accepted("smtp-main", NotifyChannel.MAIL));

        first.service.dispatch(first.outbox);
        second.service.dispatch(second.outbox);
        otherScene.service.dispatch(otherScene.outbox);

        verify(second.notifyClient, never()).send(any());
        assertEquals("RECIPIENT_MINUTE_QUOTA", second.delivery.getErrorCode());
        verify(otherScene.notifyClient).send(any());
        assertEquals("ACCEPTED", otherScene.delivery.getStatus());
    }

    @Test
    void laterLayerFailureDoesNotLeakAccountQuota() {
        MemoryQuota quota = new MemoryQuota();
        Fixture exhaustTemplate = fixture("MAIL", quota);
        Fixture leakedAttempt = fixture("MAIL", quota);
        Fixture retry = fixture("MAIL", quota);
        NotifyChannelAccount high = mailAccount(99L, "smtp-high", "Y");
        high.setMinuteMax(60);
        NotifyChannelAccount tight = mailAccount(11L, "smtp-main", "Y");
        tight.setMinuteMax(1);
        NotifySceneBinding highBinding = binding(99L, "MAIL", "${code}", "${expireMinutes}");
        highBinding.setTemplateMinuteMax(1);
        NotifySceneBinding tightBinding = binding(11L, "MAIL", "${code}", "${expireMinutes}");
        tightBinding.setTemplateMinuteMax(1);
        when(exhaustTemplate.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(highBinding);
        when(exhaustTemplate.configDao.findAccount(99L)).thenReturn(high);
        when(leakedAttempt.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(tightBinding);
        when(leakedAttempt.configDao.findAccount(11L)).thenReturn(tight);
        when(retry.configDao.findBinding("auth-captcha", "MAIL")).thenReturn(tightBinding);
        when(retry.configDao.findAccount(11L)).thenReturn(tight);
        when(exhaustTemplate.notifyClient.send(any())).thenReturn(accepted("smtp-high", NotifyChannel.MAIL));

        exhaustTemplate.service.dispatch(exhaustTemplate.outbox);
        leakedAttempt.service.dispatch(leakedAttempt.outbox);
        retry.service.dispatch(retry.outbox);

        verify(exhaustTemplate.notifyClient).send(any());
        verify(leakedAttempt.notifyClient, never()).send(any());
        verify(retry.notifyClient, never()).send(any());
        assertEquals("TEMPLATE_QUOTA", leakedAttempt.delivery.getErrorCode());
        assertEquals("TEMPLATE_QUOTA", retry.delivery.getErrorCode());
    }

    @Test
    void noticePublishedMailRendersWrapperNotCallerSnapshot() {
        Fixture fixture = fixture("MAIL");
        fixture.intent.setSceneCode("notice-published");
        fixture.intent.setTemplateCode("notice-published");
        fixture.intent.setTitleSnapshot("caller-title");
        fixture.intent.setContentSnapshot("caller-raw-body");
        fixture.intent.setTemplateParamsJson(JsonUtils.toJsonString(
            Map.of("title", "包装标题", "content", "包装正文", "path", "/n/1")));
        NotifySceneBinding binding = binding(11L, "MAIL", "外壳 ${title}", "${content}<p>${path}</p>");
        binding.setSceneCode("notice-published");
        when(fixture.configDao.findBinding("notice-published", "MAIL")).thenReturn(binding);
        when(fixture.configDao.findAccount(11L)).thenReturn(mailAccount(11L, "smtp-main", "Y"));
        when(fixture.notifyClient.send(any())).thenReturn(accepted("smtp-main", NotifyChannel.MAIL));

        fixture.service.dispatch(fixture.outbox);

        ArgumentCaptor<NotifyRequest> captor = ArgumentCaptor.forClass(NotifyRequest.class);
        verify(fixture.notifyClient).send(captor.capture());
        NotifyRichContent content = assertInstanceOf(NotifyRichContent.class, captor.getValue().content());
        assertEquals("外壳 包装标题", content.subject());
        assertEquals("包装正文<p>/n/1</p>", content.content());
        assertTrue(!content.subject().contains("caller-title"));
        assertTrue(!content.content().contains("caller-raw-body"));
    }

    private Fixture fixture(String channel) {
        return fixture(channel, (key, limit, window) -> true);
    }

    @SuppressWarnings("unchecked")
    private Fixture fixture(String channel, NotifyQuotaPort quotaPort) {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        NotifyClient notifyClient = mock(NotifyClient.class);
        ObjectProvider<InAppNotificationPort> inApp = mock(ObjectProvider.class);
        NotifyConfigDao configDao = mock(NotifyConfigDao.class);
        DispatchNotificationService service = new DispatchNotificationService(
            dao, notifyClient, inApp, configDao, quotaPort, new org.namewta.notify.usecase.NotifyDispatchResultUseCase(new NotifyDispatchResultService(dao)));
        NotifyIntent intent = new NotifyIntent();
        intent.setIntentId(1L);
        intent.setSceneCode("auth-captcha");
        intent.setTemplateCode("auth-captcha");
        intent.setBizType("auth_captcha");
        intent.setStrategy("ALL");
        intent.setTemplateParamsJson(JsonUtils.toJsonString(Map.of("code", "1234", "expireMinutes", "5")));
        NotifyDelivery delivery = new NotifyDelivery();
        delivery.setDeliveryId(2L);
        delivery.setIntentId(1L);
        delivery.setChannel(channel);
        delivery.setStatus("PENDING");
        delivery.setTargetValue("MAIL".equals(channel) ? "user@example.com" : "13812345678");
        delivery.setAttemptCount(0);
        NotifyOutbox outbox = new NotifyOutbox();
        outbox.setOutboxId(3L);
        outbox.setIntentId(1L);
        outbox.setDeliveryId(2L);
        outbox.setStatus("PROCESSING");
        outbox.setLeaseOwner("worker-1");
        outbox.setLeaseToken("token-1");
        outbox.setLeaseUntil(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));
        outbox.setAttemptCount(0);
        outbox.setMaxAttempts(5);
        when(dao.outbox(3L)).thenReturn(outbox);
        when(dao.intent(1L)).thenReturn(intent);
        when(dao.delivery(2L)).thenAnswer(invocation -> copyDelivery(delivery));
        when(dao.renewOutbox(anyLong(), anyString(), anyString())).thenReturn(1);
        when(dao.finishOutbox(any())).thenReturn(1);
        when(dao.deliveries(1L)).thenReturn(List.of(delivery));
        when(dao.databaseNow()).thenAnswer(invocation -> LocalDateTime.now(ZoneOffset.UTC));
        when(dao.lockIntent(1L)).thenReturn(intent);
        when(dao.lockDelivery(2L)).thenAnswer(invocation -> copyDelivery(delivery));
        when(dao.lockOutbox(3L)).thenReturn(outbox);
        when(dao.lockDeliveries(1L)).thenReturn(List.of(delivery));
        when(dao.saveDeliveryResult(any())).thenAnswer(invocation -> {
            org.springframework.beans.BeanUtils.copyProperties(invocation.getArgument(0), delivery);
            return 1;
        });
        when(dao.update(any(NotifyIntent.class))).thenReturn(1);
        when(dao.insert(any(org.namewta.notify.domain.entity.NotifyAttempt.class))).thenReturn(1);
        return new Fixture(service, dao, notifyClient, configDao, intent, delivery, outbox);
    }

    private NotifyDelivery copyDelivery(NotifyDelivery delivery) {
        NotifyDelivery snapshot = new NotifyDelivery();
        org.springframework.beans.BeanUtils.copyProperties(delivery, snapshot);
        return snapshot;
    }

    private NotifySceneBinding binding(Long accountId, String channel, String subject, String body) {
        NotifySceneBinding binding = new NotifySceneBinding();
        binding.setBindingId(100L);
        binding.setSceneCode("auth-captcha");
        binding.setChannel(channel);
        binding.setAccountId(accountId);
        binding.setMailSubject(subject);
        binding.setMailBody(body);
        binding.setTemplateMinuteMax(60);
        binding.setRestricted("N");
        return binding;
    }

    private NotifyChannelAccount mailAccount(Long id, String key, String enabled) {
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(id);
        account.setChannel("MAIL");
        account.setConfigKey(key);
        account.setEnabled(enabled);
        account.setMinuteMax(60);
        return account;
    }

    private NotifyChannelAccount smsAccount(Long id, String key) {
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(id);
        account.setChannel("SMS");
        account.setConfigKey(key);
        account.setEnabled("Y");
        account.setMinuteMax(60);
        return account;
    }

    private NotifyResult accepted(String providerKey, NotifyChannel channel) {
        return new NotifyResult("req", channel, providerKey, NotifyStatus.ACCEPTED, List.of());
    }

    private record Fixture(DispatchNotificationService service, NotifyNotificationDao dao, NotifyClient notifyClient,
                           NotifyConfigDao configDao, NotifyIntent intent, NotifyDelivery delivery, NotifyOutbox outbox) {
    }

    private static final class MemoryQuota implements NotifyQuotaPort {
        private final Map<String, AtomicInteger> counts = new HashMap<>();

        @Override
        public boolean tryAcquire(String key, int limit, Duration window) {
            if (limit <= 0) {
                return true;
            }
            int value = counts.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
            if (value <= limit) {
                return true;
            }
            counts.get(key).decrementAndGet();
            return false;
        }

        @Override
        public void release(String key) {
            AtomicInteger counter = counts.get(key);
            if (counter != null && counter.get() > 0) {
                counter.decrementAndGet();
            }
        }
    }
}
