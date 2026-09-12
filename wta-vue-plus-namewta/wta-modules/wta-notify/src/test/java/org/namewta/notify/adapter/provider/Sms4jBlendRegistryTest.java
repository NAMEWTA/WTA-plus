package org.namewta.notify.adapter.provider;

import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.tencent.config.TencentConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 厂商 Config 必须是 AlibabaConfig/TencentConfig，否则 SMS4J Factory checkcast 失败。
 */
@Tag("dev")
class Sms4jBlendRegistryTest {

    private final Sms4jBlendRegistry registry = new Sms4jBlendRegistry();

    @AfterEach
    void unregisterBlends() {
        registry.remove("ali-prod");
        registry.remove("tx-prod");
    }

    @Test
    void vendorConfigUsesAlibabaAndTencentClassesNotGenericBaseConfig() {
        BaseConfig alibaba = registry.vendorConfig(smsAccount("ali-prod", "alibaba"));
        BaseConfig tencent = registry.vendorConfig(smsAccount("tx-prod", "tencent"));

        assertInstanceOf(AlibabaConfig.class, alibaba);
        assertInstanceOf(TencentConfig.class, tencent);
        assertEquals("ali-prod", alibaba.getConfigId());
        assertEquals("ak-ali", alibaba.getAccessKeyId());
        assertEquals("sk-ali", alibaba.getAccessKeySecret());
        assertEquals("签名", alibaba.getSignature());
        assertEquals("app-ali", alibaba.getSdkAppId());
        assertEquals("alibaba", alibaba.getSupplier());
        assertEquals("tencent", tencent.getSupplier());
        assertEquals("tx-prod", tencent.getConfigId());
        assertNotEquals(alibaba.getClass(), BaseConfig.class);
        assertTrue(AlibabaConfig.class.isAssignableFrom(alibaba.getClass()));
    }

    @Test
    void upsertRegistersBlendRetrievableByConfigKeyWithoutClassCast() {
        assertDoesNotThrow(() -> registry.upsert(smsAccount("ali-prod", "alibaba")));
        SmsBlend blend = SmsFactory.getSmsBlend("ali-prod");
        assertNotNull(blend);
        assertEquals("ali-prod", blend.getConfigId());
        assertEquals("alibaba", blend.getSupplier());

        assertDoesNotThrow(() -> registry.upsert(smsAccount("tx-prod", "tencent")));
        SmsBlend tencent = SmsFactory.getSmsBlend("tx-prod");
        assertNotNull(tencent);
        assertEquals("tx-prod", tencent.getConfigId());
        assertEquals("tencent", tencent.getSupplier());
    }

    @Test
    void removeUnregistersBlend() {
        registry.upsert(smsAccount("ali-prod", "alibaba"));
        assertNotNull(SmsFactory.getSmsBlend("ali-prod"));

        registry.remove("ali-prod");
        assertNull(SmsFactory.getSmsBlend("ali-prod"));
    }

    private NotifyChannelAccount smsAccount(String configKey, String supplier) {
        NotifyChannelAccount account = new NotifyChannelAccount();
        account.setChannel("SMS");
        account.setConfigKey(configKey);
        account.setSupplier(supplier);
        account.setAccessKeyId("ak-" + supplier.substring(0, 3));
        account.setAccessKeySecret("sk-" + supplier.substring(0, 3));
        account.setSignature("签名");
        account.setSdkAppId("app-" + supplier.substring(0, 3));
        account.setEnabled("Y");
        account.setMinuteMax(60);
        return account;
    }
}
