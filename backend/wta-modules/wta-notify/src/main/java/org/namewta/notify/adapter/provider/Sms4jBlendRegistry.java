package org.namewta.notify.adapter.provider;

import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.port.SmsBlendRegistryPort;
import org.namewta.common.sms.notify.SmsSingleAttemptBlendVerifier;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.aliyun.config.AlibabaFactory;
import org.dromara.sms4j.api.universal.SupplierConfig;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.core.proxy.SmsProxyFactory;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.provider.factory.BeanFactory;
import org.dromara.sms4j.provider.factory.BaseProviderFactory;
import org.dromara.sms4j.provider.factory.ProviderFactoryHolder;
import org.dromara.sms4j.tencent.config.TencentFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 把短信渠道账号注册为 SMS4J blend。
 *
 * <p>必须实例化厂商自己的 Config（如 {@code AlibabaConfig}），不能用匿名 {@code BaseConfig} 子类，
 * 否则 {@code AlibabaFactory.createSms} 会 ClassCastException。</p>
 */
@Component
public class Sms4jBlendRegistry implements SmsBlendRegistryPort, SmsSingleAttemptBlendVerifier {

    private final ConcurrentHashMap<String, SmsBlend> singleAttemptBlends = new ConcurrentHashMap<>();

    static {
        ProviderFactoryHolder.registerFactory(AlibabaFactory.instance());
        ProviderFactoryHolder.registerFactory(TencentFactory.instance());
    }

    /**
     * 注册或更新。
     *
     * @param account 短信账号
     */
    @Override
    public synchronized void upsert(NotifyChannelAccount account) {
        if (account == null || account.getConfigKey() == null) {
            return;
        }
        remove(account.getConfigKey());
        BeanFactory.getSmsConfig();
        BaseConfig config = vendorConfig(account);
        // SMS4J 3.3.5 会对任何失败（包括传输结果不明）内部重发；本账号只能单次请求。
        config.setMaxRetries(0);
        BaseProviderFactory<?, ?> factory = ProviderFactoryHolder.requireForSupplier(config.getSupplier());
        SmsBlend selected = createOwnedBlend(factory, config);
        try {
            SmsFactory.register(selected);
            // 保存创建时的确切对象引用，不能在注册后重新从可被外部覆盖的全局 Map 读取并认证。
            singleAttemptBlends.put(config.getConfigId(), selected);
        } catch (RuntimeException failure) {
            singleAttemptBlends.remove(config.getConfigId());
            throw failure;
        }
    }

    private <C extends SupplierConfig> SmsBlend createOwnedBlend(
        BaseProviderFactory<? extends SmsBlend, C> factory, BaseConfig config) {
        C typed = factory.getConfigClass().cast(config);
        // 与 SmsFactory.createSmsBlend 内部相同的 provider factory + SMS4J proxy 装配路径。
        return SmsProxyFactory.getProxySmsBlend(factory.createSms(typed));
    }

    @Override
    public boolean isSingleAttempt(SmsBlend selectedBlend) {
        return selectedBlend != null && selectedBlend == singleAttemptBlends.get(selectedBlend.getConfigId());
    }

    /**
     * 按厂商标识构造 SMS4J 配置对象。
     *
     * @param account 短信账号
     * @return 厂商 Config，供 {@link SmsFactory#createSmsBlend(SupplierConfig)} 使用
     */
    public BaseConfig vendorConfig(NotifyChannelAccount account) {
        if (account == null || isBlank(account.getSupplier())) {
            throw new IllegalArgumentException("短信厂商标识不能为空");
        }
        String supplier = account.getSupplier().trim();
        BaseProviderFactory<?, ?> factory = ProviderFactoryHolder.requireForSupplier(supplier);
        if (factory == null || factory.getConfigClass() == null) {
            throw new IllegalArgumentException("不支持的短信厂商 " + supplier);
        }
        try {
            Object created = factory.getConfigClass().getDeclaredConstructor().newInstance();
            if (!(created instanceof BaseConfig config)) {
                throw new IllegalArgumentException("短信厂商配置类型无效 " + supplier);
            }
            config.setConfigId(account.getConfigKey());
            config.setAccessKeyId(account.getAccessKeyId());
            config.setAccessKeySecret(account.getAccessKeySecret());
            config.setSignature(account.getSignature());
            config.setSdkAppId(account.getSdkAppId());
            return config;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("无法创建短信厂商配置 " + supplier, exception);
        }
    }

    /**
     * 注销。
     *
     * @param configKey 账号标识
     */
    @Override
    public synchronized void remove(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            return;
        }
        singleAttemptBlends.remove(configKey);
        try {
            SmsFactory.unregister(configKey);
        } catch (RuntimeException ignored) {
            // SmsLoad 未初始化时仍从 BLENDS 表删除；失败不影响停用语义。
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
