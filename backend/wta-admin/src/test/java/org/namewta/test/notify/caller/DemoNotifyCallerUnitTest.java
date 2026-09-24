package org.namewta.test.notify.caller;

import org.namewta.notify.api.*;
import org.namewta.demo.controller.MailSendController;
import org.namewta.demo.controller.SmsController;
import org.namewta.demo.controller.WebSocketController;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.support.NotifySendPlanner;
import org.namewta.notify.support.NotifyTemplateRenderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class DemoNotifyCallerUnitTest {

    @Test
    void mailDemoUsesOssIdsForAttachmentSnapshots() {
        NotificationApplicationService notificationService = acceptingService();
        MailSendController controller = new MailSendController(notificationService);

        controller.sendSimpleMessage("user@example.com", "主题", "正文");
        controller.sendMessageWithAttachment("user@example.com", "主题", "正文", 77L);
        controller.sendMessageWithAttachments("user@example.com", "主题", "正文", List.of(77L, 88L));

        ArgumentCaptor<NotificationCommand> requests = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService, times(3)).submit(requests.capture());
        assertTrue(requests.getAllValues().stream().allMatch(request -> request.priority() == 0));
        assertEquals(List.of(), requests.getAllValues().get(0).templateParams().get("attachmentOssIds"));
        assertEquals(List.of(77L), requests.getAllValues().get(1).templateParams().get("attachmentOssIds"));
        assertEquals(List.of(77L, 88L), requests.getAllValues().get(2).templateParams().get("attachmentOssIds"));
        assertTrue(requests.getAllValues().stream().allMatch(request -> request.channels().equals(List.of(NotificationChannel.MAIL))));
    }

    @Test
    void mailDemoBodyWithoutLinkPassesRealPlannerWhileNoticeStillNeedsPath() {
        NotificationApplicationService notificationService = acceptingService();
        new MailSendController(notificationService).sendSimpleMessage("user@example.com", "主题", "正文");
        ArgumentCaptor<NotificationCommand> captured = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService).submit(captured.capture());
        NotificationCommand command = captured.getValue();
        NotifySceneBinding binding = new NotifySceneBinding();
        binding.setAccountId(11L);
        binding.setMailSubject("${title}");
        binding.setMailBody("${content}");
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(11L);
        account.setChannel("MAIL");
        account.setConfigKey("owned-mail");
        account.setEnabled("Y");

        var planned = NotifySendPlanner.preflight(command.sceneCode(), "MAIL",
            NotifyTemplateRenderer.stringify(command.templateParams()), binding, account);
        assertTrue(planned.ok(), () -> "正文邮件不得因缺少链接失败：" + planned.errorCode());
        assertEquals("demo-mail", command.sceneCode());
        assertEquals("主题", planned.subject());
        assertEquals("正文", planned.body());
        var protectedNotice = NotifySendPlanner.preflight("notice-published", "MAIL",
            NotifyTemplateRenderer.stringify(command.templateParams()), binding, account);
        assertFalse(protectedNotice.ok());
        assertEquals("MISSING_VARIABLE", protectedNotice.errorCode());
    }

    @Test
    void smsDemoUsesExplicitProviderAndCompleteTemplateSnapshot() {
        NotificationApplicationService notificationService = acceptingService();
        SmsController controller = new SmsController(notificationService);

        controller.sendAliyun("13812345678,13912345678", "TPL-A");
        controller.sendTencent("13712345678", "TPL-T");

        ArgumentCaptor<NotificationCommand> requests = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService, times(2)).submit(requests.capture());
        NotificationCommand aliyun = requests.getAllValues().get(0);
        NotificationCommand tencent = requests.getAllValues().get(1);
        assertEquals(0, aliyun.priority());
        assertEquals(0, tencent.priority());
        assertAll(
            () -> assertEquals("auth-captcha", aliyun.templateCode()),
            () -> assertEquals(List.of("13812345678", "13912345678"), aliyun.recipientIds()),
            () -> assertEquals("1234", aliyun.templateParams().get("code")),
            () -> assertFalse(aliyun.templateParams().containsKey("providerKey")),
            () -> assertFalse(aliyun.templateParams().containsKey("content")),
            () -> assertEquals("auth-captcha", tencent.templateCode()),
            () -> assertEquals("1234", tencent.templateParams().get("code"))
        );
    }

    @Test
    void websocketDemoKeepsRecipientSelectionWithSupportedPriority() {
        NotificationApplicationService notificationService = acceptingService();
        WebSocketController controller = new WebSocketController(notificationService);
        controller.send(7L, "owned text");
        controller.send(null, "owned broadcast");

        ArgumentCaptor<NotificationCommand> requests = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService, times(2)).submit(requests.capture());
        assertEquals("USER", requests.getAllValues().get(0).recipientType());
        assertEquals(List.of("7"), requests.getAllValues().get(0).recipientIds());
        assertEquals("ALL", requests.getAllValues().get(1).recipientType());
        assertEquals(List.of(), requests.getAllValues().get(1).recipientIds());
        assertTrue(requests.getAllValues().stream().allMatch(request -> request.priority() == 0));
    }

    private NotificationApplicationService acceptingService() {
        NotificationApplicationService service = mock(NotificationApplicationService.class);
        when(service.submit(any())).thenReturn(new NotificationReceipt("notification-1", NotificationStatus.ACCEPTED,
            false, false, List.of()));
        return service;
    }
}
