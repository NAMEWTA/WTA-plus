package org.namewta.notify.service;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.domain.bo.NotifyChannelAccountBo;
import org.namewta.notify.domain.bo.NotifySceneBindingBo;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.domain.vo.NotifyChannelAccountVo;
import org.namewta.notify.port.SmsBlendRegistryPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLSyntaxErrorException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 渠道账号密钥不回显、空白保持原值，模板限额不能超过账号限额。
 */
@Tag("dev")
class NotifyConfigServiceTest {

    @Test
    void missingChannelAccountTableDoesNotAbortStartup() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        SmsBlendRegistryPort registry = mock(SmsBlendRegistryPort.class);
        when(dao.listAccounts("SMS")).thenThrow(new BadSqlGrammarException(
            "select",
            "SELECT * FROM notify_channel_account",
            new SQLSyntaxErrorException("Table 'ry-namewta.notify_channel_account' doesn't exist")));
        NotifyConfigService service = new NotifyConfigService(dao, registry);

        assertDoesNotThrow(service::loadEnabledSmsAccounts);
        verify(registry, never()).upsert(any());
        verify(registry, never()).remove(any());
    }

    @Test
    void accountVoOmitsSecretFields() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotifyConfigService service = new NotifyConfigService(dao, mock(SmsBlendRegistryPort.class));
        NotifyChannelAccount account = account();
        account.setMailPass("smtp-secret");
        account.setAccessKeySecret("sms-secret");
        when(dao.findAccount(8L)).thenReturn(account);

        NotifyChannelAccountVo vo = service.getAccount(8L);
        String json = JsonUtils.toJsonString(vo);

        assertTrue(vo.isMailPassSet());
        assertTrue(vo.isAccessKeySecretSet());
        assertFalse(json.contains("smtp-secret"));
        assertFalse(json.contains("sms-secret"));
        assertFalse(json.contains("\"mailPass\":"));
        assertFalse(json.contains("\"accessKeySecret\":"));
    }

    @Test
    void blankSecretOnEditKeepsPreviousValue() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotifyConfigService service = new NotifyConfigService(dao, mock(SmsBlendRegistryPort.class));
        NotifyChannelAccount current = account();
        current.setMailPass("keep-me");
        current.setAccessKeySecret("keep-sms");
        when(dao.findAccount(8L)).thenReturn(current);
        when(dao.update(any(NotifyChannelAccount.class))).thenReturn(1);

        NotifyChannelAccountBo bo = new NotifyChannelAccountBo();
        bo.setAccountId(8L);
        bo.setChannel("MAIL");
        bo.setConfigKey("smtp-main");
        bo.setEnabled("Y");
        bo.setMinuteMax(30);
        bo.setHost("smtp.example.com");
        bo.setPort(465);
        bo.setMailFrom("ops@example.com");
        bo.setMailPass("");
        bo.setAccessKeySecret(" ");
        service.updateAccount(bo);

        ArgumentCaptor<NotifyChannelAccount> captor = ArgumentCaptor.forClass(NotifyChannelAccount.class);
        verify(dao).update(captor.capture());
        assertEquals("keep-me", captor.getValue().getMailPass());
        assertEquals("keep-sms", captor.getValue().getAccessKeySecret());
    }

    @Test
    void templateMinuteMaxCannotExceedAccountCap() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotifyConfigService service = new NotifyConfigService(dao, mock(SmsBlendRegistryPort.class));
        NotifyChannelAccount account = account();
        account.setMinuteMax(10);
        when(dao.findAccount(8L)).thenReturn(account);

        NotifySceneBindingBo bo = new NotifySceneBindingBo();
        bo.setSceneCode("auth-captcha");
        bo.setChannel("MAIL");
        bo.setAccountId(8L);
        bo.setMailSubject("码 ${code}");
        bo.setMailBody("${expireMinutes}");
        bo.setTemplateMinuteMax(20);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.saveBinding(bo));
        assertTrue(exception.getMessage().contains("不能超过账号上限"));
    }

    @Test
    void disablingSmsAccountUnregistersBlend() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        SmsBlendRegistryPort registry = mock(SmsBlendRegistryPort.class);
        NotifyConfigService service = new NotifyConfigService(dao, registry);
        NotifyChannelAccount current = new NotifyChannelAccount();
        current.setAccountId(8L);
        current.setChannel("SMS");
        current.setConfigKey("ali-prod");
        current.setEnabled("Y");
        current.setMinuteMax(30);
        when(dao.findAccount(8L)).thenReturn(current);
        when(dao.update(any(NotifyChannelAccount.class))).thenReturn(1);

        service.changeStatus(8L, "N");

        verify(registry).remove("ali-prod");
        verify(registry, never()).upsert(any());
    }

    @Test
    void mailBindingRejectsRenamedRequiredTokenAndAcceptsMovedToken() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotifyConfigService service = new NotifyConfigService(dao, mock(SmsBlendRegistryPort.class));
        when(dao.findAccount(8L)).thenReturn(account());
        when(dao.findBinding("auth-captcha", "MAIL")).thenReturn(null);
        when(dao.insert(any(NotifySceneBinding.class))).thenReturn(1);

        NotifySceneBindingBo renamed = new NotifySceneBindingBo();
        renamed.setSceneCode("auth-captcha");
        renamed.setChannel("MAIL");
        renamed.setAccountId(8L);
        renamed.setMailSubject("码 ${code}");
        renamed.setMailBody("分钟 ${minutes}");
        renamed.setTemplateMinuteMax(10);
        ServiceException missing = assertThrows(ServiceException.class, () -> service.saveBinding(renamed));
        assertTrue(missing.getMessage().contains("expireMinutes") || missing.getMessage().contains("未声明"));

        NotifySceneBindingBo moved = new NotifySceneBindingBo();
        moved.setSceneCode("auth-captcha");
        moved.setChannel("MAIL");
        moved.setAccountId(8L);
        moved.setMailSubject("${expireMinutes}");
        moved.setMailBody("验证码 ${code}");
        moved.setTemplateMinuteMax(10);
        assertEquals(1, service.saveBinding(moved));
    }

    @Test
    void cannotEnableMailAccountWithoutSecret() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotifyConfigService service = new NotifyConfigService(dao, mock(SmsBlendRegistryPort.class));
        when(dao.findAccount("MAIL", "smtp-empty")).thenReturn(null);

        NotifyChannelAccountBo bo = new NotifyChannelAccountBo();
        bo.setChannel("MAIL");
        bo.setConfigKey("smtp-empty");
        bo.setEnabled("Y");
        bo.setMinuteMax(10);
        bo.setHost("smtp.example.com");
        bo.setPort(465);
        bo.setMailFrom("ops@example.com");
        ServiceException add = assertThrows(ServiceException.class, () -> service.addAccount(bo));
        assertTrue(add.getMessage().contains("密码"));
        verify(dao, never()).insert(any(NotifyChannelAccount.class));

        NotifyChannelAccount current = account();
        current.setMailPass("");
        when(dao.findAccount(8L)).thenReturn(current);
        ServiceException enable = assertThrows(ServiceException.class, () -> service.changeStatus(8L, "Y"));
        assertTrue(enable.getMessage().contains("密码"));
        verify(dao, never()).update(any(NotifyChannelAccount.class));
    }

    @Test
    void cannotEnableSmsAccountWithoutSecret() {
        NotifyConfigDao dao = mock(NotifyConfigDao.class);
        NotifyConfigService service = new NotifyConfigService(dao, mock(SmsBlendRegistryPort.class));
        when(dao.findAccount("SMS", "ali-empty")).thenReturn(null);

        NotifyChannelAccountBo bo = new NotifyChannelAccountBo();
        bo.setChannel("SMS");
        bo.setConfigKey("ali-empty");
        bo.setEnabled("Y");
        bo.setMinuteMax(10);
        bo.setSupplier("alibaba");
        bo.setAccessKeyId("ak");
        ServiceException add = assertThrows(ServiceException.class, () -> service.addAccount(bo));
        assertTrue(add.getMessage().contains("密钥"));
        verify(dao, never()).insert(any(NotifyChannelAccount.class));
    }

    private NotifyChannelAccount account() {
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(8L);
        account.setChannel("MAIL");
        account.setConfigKey("smtp-main");
        account.setEnabled("Y");
        account.setMinuteMax(30);
        account.setHost("smtp.example.com");
        account.setPort(465);
        account.setMailFrom("ops@example.com");
        account.setMailPass("keep-me");
        return account;
    }
}
