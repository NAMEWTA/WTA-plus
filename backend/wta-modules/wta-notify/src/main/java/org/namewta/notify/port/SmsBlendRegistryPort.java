package org.namewta.notify.port;

import org.namewta.notify.domain.entity.NotifyChannelAccount;

/**
 * 将短信渠道账号注册到 SMS4J。
 */
public interface SmsBlendRegistryPort {

    /**
     * 注册或更新 blend。
     *
     * @param account 短信账号
     */
    void upsert(NotifyChannelAccount account);

    /**
     * 注销 blend。
     *
     * @param configKey 账号标识
     */
    void remove(String configKey);
}
