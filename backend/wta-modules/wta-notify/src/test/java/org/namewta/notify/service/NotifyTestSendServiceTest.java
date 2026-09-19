package org.namewta.notify.service;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试发送不得绕过停用或未绑定。
 */
@Tag("dev")
class NotifyTestSendServiceTest {

    @Test
    void disabledAccountTestSendFailsClosed() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(1L);
        account.setEnabled("N");
        when(dao.findAccount(1L)).thenReturn(account);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> new NotifyTestSendService(dao, notifications).sendAccount(1L, "auth-captcha", "a@b.c"));
        assertTrue(exception.getMessage().contains("未启用"));
    }

    @Test
    void unboundTemplateTestSendFailsClosed() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        when(dao.findBinding("auth-captcha", "MAIL")).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
            () -> new NotifyTestSendService(dao, notifications).sendTemplate("auth-captcha", "MAIL", "a@b.c"));
        assertTrue(exception.getMessage().contains("未绑定"));
    }

    @Test
    void templateTestSendSubmitsSceneVariablesWithoutProviderKey() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotificationApplicationService notifications = mock(NotificationApplicationService.class);
        NotifySceneBinding binding = new NotifySceneBinding();
        binding.setAccountId(9L);
        binding.setSceneCode("auth-captcha");
        binding.setChannel("MAIL");
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(9L);
        account.setEnabled("Y");
        account.setChannel("MAIL");
        when(dao.findBinding("auth-captcha", "MAIL")).thenReturn(binding);
        when(dao.findAccount(9L)).thenReturn(account);
        when(notifications.submit(any())).thenReturn(
            new NotificationReceipt("n1", NotificationStatus.QUEUED, true, false, List.of()));

        String status = new NotifyTestSendService(dao, notifications)
            .sendTemplate("auth-captcha", "MAIL", "user@example.com");

        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notifications).submit(captor.capture());
        assertEquals("QUEUED", status);
        assertEquals("auth-captcha", captor.getValue().templateCode());
        assertTrue(captor.getValue().templateParams().containsKey("code"));
        assertEquals(false, captor.getValue().templateParams().containsKey("providerKey"));
    }
}
