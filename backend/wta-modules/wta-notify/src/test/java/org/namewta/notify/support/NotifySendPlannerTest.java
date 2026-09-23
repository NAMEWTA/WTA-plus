package org.namewta.notify.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 收件人限额键使用规范化 SHA-256 摘要，而不是 {@code hashCode}。
 */
@Tag("dev")
class NotifySendPlannerTest {

    @Test
    void recipientQuotaTokenIsStableDigestNotHashCode() {
        String mixed = NotifySendPlanner.recipientQuotaToken(" User@Example.com ");
        String folded = NotifySendPlanner.recipientQuotaToken("user@example.com");
        String other = NotifySendPlanner.recipientQuotaToken("other@example.com");
        String hashCodeHex = Integer.toHexString("user@example.com".hashCode());

        assertEquals(mixed, folded);
        assertEquals(16, mixed.length());
        assertNotEquals(other, folded);
        assertNotEquals(hashCodeHex, folded);
        assertEquals(folded, NotifySendPlanner.recipientQuotaToken("user@example.com"));
    }

    @Test
    void invalidMappedVariableFailsBeforeQuotaAndProviderParamsRemainExact() {
        NotifySceneBinding binding = new NotifySceneBinding();
        binding.setAccountId(10L);
        binding.setSmsTemplateCode("SMS_TEMPLATE");
        binding.setSmsParamMappingJson("{\"code\":\"1\",\"expireMinutes\":\"2\",\"extra\":\"3\"}");
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(10L);
        account.setChannel("SMS");
        account.setEnabled("Y");
        account.setSupplier("tencent");
        account.setConfigKey("sms-owned");
        account.setMinuteMax(2);
        AtomicInteger quotaCalls = new AtomicInteger();
        var quota = (org.namewta.notify.port.NotifyQuotaPort) (key, limit, window) -> {
            quotaCalls.incrementAndGet(); return true;
        };
        var invalid = NotifySendPlanner.plan("auth-captcha", "SMS", "13812345678",
            Map.of("code", "1234", "expireMinutes", "5"), binding, account, quota);
        assertFalse(invalid.ok());
        assertEquals("INVALID_TEMPLATE_PARAMETERS", invalid.errorCode());
        assertEquals(0, quotaCalls.get());

        var valid = NotifySendPlanner.plan("auth-captcha", "SMS", "13812345678",
            Map.of("code", "1234", "expireMinutes", "5", "extra", "value"), binding, account, quota);
        assertTrue(valid.ok());
        assertEquals(Map.of("1", "1234", "2", "5", "3", "value"), valid.smsParams());
        assertFalse(valid.smsSnapshot().contains("1234"));
        assertFalse(valid.smsSnapshot().contains("13812345678"));
        assertTrue(quotaCalls.get() > 0);
    }

    @Test
    void laterQuotaExceptionReleasesEarlierHeldAccountSlot() {
        NotifySceneBinding binding = new NotifySceneBinding();
        binding.setAccountId(10L);
        binding.setSmsTemplateCode("SMS_TEMPLATE");
        binding.setSmsParamMappingJson("{\"code\":\"code\",\"expireMinutes\":\"min\"}");
        binding.setTemplateMinuteMax(10);
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(10L);
        account.setChannel("SMS");
        account.setEnabled("Y");
        account.setConfigKey("sms-owned");
        account.setMinuteMax(10);
        AtomicInteger heldAccount = new AtomicInteger();
        var quota = new org.namewta.notify.port.NotifyQuotaPort() {
            @Override public boolean tryAcquire(String key, int limit, java.time.Duration window) {
                if (key.startsWith("acct:")) { heldAccount.incrementAndGet(); return true; }
                throw new IllegalStateException("synthetic template quota failure");
            }
            @Override public void release(String key) {
                if (key.startsWith("acct:")) heldAccount.decrementAndGet();
            }
        };

        assertThrows(IllegalStateException.class, () -> NotifySendPlanner.plan("auth-captcha", "SMS",
            "13812345678", Map.of("code", "1234", "expireMinutes", "5"), binding, account, quota));
        assertEquals(0, heldAccount.get());
    }

    @Test
    void quotaReleaseFailureIsSuppressedWithoutReplacingOriginalFailure() {
        NotifySceneBinding binding = new NotifySceneBinding();
        binding.setAccountId(10L);
        binding.setSmsTemplateCode("SMS_TEMPLATE");
        binding.setSmsParamMappingJson("{\"code\":\"code\",\"expireMinutes\":\"min\"}");
        binding.setTemplateMinuteMax(10);
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setAccountId(10L);
        account.setChannel("SMS");
        account.setEnabled("Y");
        account.setConfigKey("sms-owned");
        account.setMinuteMax(10);
        var quota = new org.namewta.notify.port.NotifyQuotaPort() {
            @Override public boolean tryAcquire(String key, int limit, java.time.Duration window) {
                if (key.startsWith("acct:")) return true;
                throw new IllegalStateException("original template outage");
            }
            @Override public void release(String key) {
                throw new IllegalStateException("account release outage");
            }
        };

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> NotifySendPlanner.plan(
            "auth-captcha", "SMS", "13812345678", Map.of("code", "1234", "expireMinutes", "5"),
            binding, account, quota));
        assertEquals("original template outage", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("account release outage", failure.getSuppressed()[0].getMessage());
    }
}
