package org.namewta.test.notify.caller;

import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.constant.GlobalConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.web.config.properties.CaptchaProperties;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.web.controller.CaptchaController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.time.Instant;

import static org.namewta.common.core.constant.HttpStatus.ERROR;
import static org.namewta.common.core.constant.HttpStatus.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class CaptchaNotifyCallerUnitTest {

    @Test
    void smsCaptchaUsesTemplateSnapshotAndCachesAfterQueuedSubmission() {
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        when(notificationService.submit(any())).thenReturn(queued());
        RecordingCaptchaController controller = new RecordingCaptchaController(
            new CaptchaProperties(), notificationService);

        var response = controller.smsCode("13812345678");

        assertEquals(SUCCESS, response.getCode());
        ArgumentCaptor<NotificationCommand> request = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService).submit(request.capture());
        String code = String.valueOf(request.getValue().templateParams().get("code"));
        assertAll(
            () -> assertEquals("PHONE", request.getValue().recipientType()),
            () -> assertEquals("13812345678", request.getValue().recipientIds().getFirst()),
            () -> assertEquals("13812345678", request.getValue().bizId()),
            () -> assertEquals("auth-captcha", request.getValue().templateCode()),
            () -> assertNotNull(request.getValue().expiresAt(), "短信验证码必须携带绝对截止时间"),
            () -> assertEquals(request.getValue().expiresAt(), controller.cachedDeadline),
            () -> assertTrue(request.getValue().idempotencyKey().startsWith("captcha:sms:13812345678:")),
            () -> assertEquals(code, String.valueOf(request.getValue().templateParams().get("code"))),
            () -> assertEquals(String.valueOf(Constants.CAPTCHA_EXPIRATION),
                String.valueOf(request.getValue().templateParams().get("expireMinutes"))),
            () -> assertFalse(request.getValue().templateParams().containsKey("content")),
            () -> assertEquals(GlobalConstants.CAPTCHA_CODE_KEY + "13812345678", controller.cachedKey),
            () -> assertEquals(code, controller.cachedCode)
        );
    }

    @Test
    void smsCaptchaReturnsFailureAndDoesNotCacheWhenSubmissionFails() {
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        when(notificationService.submit(any())).thenThrow(new IllegalStateException("provider rejected"));
        RecordingCaptchaController controller = new RecordingCaptchaController(
            new CaptchaProperties(), notificationService);

        var response = controller.smsCode("13812345678");

        assertEquals(ERROR, response.getCode());
        assertEquals("验证码短信发送失败", response.getMsg());
        assertNull(controller.cachedKey);
        assertNull(controller.cachedCode);
    }

    @Test
    void emailCaptchaUsesNotifyClientAndCachesAfterQueuedSubmission() {
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        when(notificationService.submit(any())).thenReturn(queued());
        RecordingCaptchaController controller = new RecordingCaptchaController(
            new CaptchaProperties(), notificationService);

        controller.emailCodeImpl("user@example.com");

        ArgumentCaptor<NotificationCommand> request = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService).submit(request.capture());
        assertAll(
            () -> assertEquals("EMAIL", request.getValue().recipientType()),
            () -> assertEquals("user@example.com", request.getValue().recipientIds().getFirst()),
            () -> assertEquals("auth-captcha", request.getValue().templateCode()),
            () -> assertNotNull(request.getValue().expiresAt(), "邮件验证码必须携带绝对截止时间"),
            () -> assertEquals(request.getValue().expiresAt(), controller.cachedDeadline),
            () -> assertTrue(request.getValue().idempotencyKey().startsWith("captcha:mail:user@example.com:")),
            () -> assertEquals(controller.cachedCode, String.valueOf(request.getValue().templateParams().get("code"))),
            () -> assertEquals(String.valueOf(Constants.CAPTCHA_EXPIRATION),
                String.valueOf(request.getValue().templateParams().get("expireMinutes"))),
            () -> assertFalse(request.getValue().templateParams().containsKey("content")),
            () -> assertEquals(GlobalConstants.CAPTCHA_CODE_KEY + "user@example.com", controller.cachedKey)
        );
    }

    @Test
    void emailCaptchaDoesNotCacheWhenNotificationFails() {
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        when(notificationService.submit(any())).thenThrow(new IllegalStateException("provider secret"));
        RecordingCaptchaController controller = new RecordingCaptchaController(
            new CaptchaProperties(), notificationService);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> controller.emailCodeImpl("user@example.com"));

        assertEquals("验证码邮件发送失败", exception.getMessage());
        assertNull(controller.cachedKey);
        assertNull(controller.cachedCode);
    }

    @Test
    void twoCodesInSameMinuteHaveDifferentIdempotencyKeys() {
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        when(notificationService.submit(any())).thenReturn(queued());
        RecordingCaptchaController controller = new RecordingCaptchaController(
            new CaptchaProperties(), notificationService);

        assertEquals(SUCCESS, controller.smsCode("13812345678").getCode());
        assertEquals(SUCCESS, controller.smsCode("13812345678").getCode());

        ArgumentCaptor<NotificationCommand> commands = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService, times(2)).submit(commands.capture());
        assertNotEquals(commands.getAllValues().get(0).idempotencyKey(),
            commands.getAllValues().get(1).idempotencyKey());
        assertEquals(commands.getAllValues().get(0).expiresAt(), commands.getAllValues().get(1).expiresAt());
    }

    @Test
    void delayedSubmitCannotCacheSmsBeyondOriginalDeadline() {
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        RecordingCaptchaController controller = new RecordingCaptchaController(new CaptchaProperties(), notifications);
        when(notifications.submit(any())).thenAnswer(call -> {
            controller.now = controller.now.plusSeconds(121);
            return queued();
        });

        assertEquals(ERROR, controller.smsCode("13812345678").getCode());
        ArgumentCaptor<NotificationCommand> command = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notifications).submit(command.capture());
        assertEquals(Instant.parse("2026-09-23T12:36:10Z"), command.getValue().expiresAt());
        assertNull(controller.cachedCode);
    }

    @Test
    void redisFailureCannotProduceSuccessfulEmailCaptcha() {
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        when(notifications.submit(any())).thenReturn(queued());
        RecordingCaptchaController controller = new RecordingCaptchaController(new CaptchaProperties(), notifications);
        controller.cacheResult = false;

        assertThrows(ServiceException.class, () -> controller.emailCodeImpl("user@example.com"));
        verify(notifications).submit(any());
        assertNull(controller.cachedCode);
    }

    @Test
    void redisFailureCannotProduceSuccessfulSmsCaptcha() {
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        when(notifications.submit(any())).thenReturn(queued());
        RecordingCaptchaController controller = new RecordingCaptchaController(new CaptchaProperties(), notifications);
        controller.cacheResult = false;

        assertEquals(ERROR, controller.smsCode("13812345678").getCode());
        verify(notifications).submit(any());
        assertNull(controller.cachedCode);
    }

    @Test
    void delayedSubmitCannotCacheEmailBeyondOriginalDeadline() {
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        RecordingCaptchaController controller = new RecordingCaptchaController(new CaptchaProperties(), notifications);
        when(notifications.submit(any())).thenAnswer(call -> {
            controller.now = controller.now.plusSeconds(121);
            return queued();
        });

        assertThrows(ServiceException.class, () -> controller.emailCodeImpl("user@example.com"));
        ArgumentCaptor<NotificationCommand> command = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notifications).submit(command.capture());
        assertEquals(Instant.parse("2026-09-23T12:36:10Z"), command.getValue().expiresAt());
        assertNull(controller.cachedCode);
    }

    private NotificationReceipt queued() {
        return new NotificationReceipt("notification-1", NotificationStatus.QUEUED, true, false, List.of());
    }

    private static final class RecordingCaptchaController extends CaptchaController {

        private String cachedKey;
        private String cachedCode;
        private Instant cachedDeadline;
        private Instant now = Instant.parse("2026-09-23T12:34:10.500Z");
        private boolean cacheResult = true;

        private RecordingCaptchaController(CaptchaProperties captchaProperties,
                                           NotificationApplicationService notificationService) {
            super(captchaProperties, notificationService);
        }

        @Override
        protected Instant captchaNow() { return now; }

        @Override
        protected boolean cacheCaptchaCode(String key, String code, Instant deadline) {
            if (!cacheResult || !deadline.isAfter(now)) return false;
            cachedKey = key;
            cachedCode = code;
            cachedDeadline = deadline;
            return true;
        }
    }
}
