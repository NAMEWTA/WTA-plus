package org.namewta.common.sms.config;

import org.namewta.common.sms.core.dao.PlusSmsDao;
import org.namewta.common.sms.handler.SmsExceptionHandler;
import org.namewta.common.sms.notify.Sms4jNotificationProviderResolver;
import org.namewta.common.sms.notify.SmsNotificationProviderResolver;
import org.namewta.common.sms.notify.SmsNotifyChannelAdapter;
import org.namewta.common.sms.notify.SmsSingleAttemptBlendVerifier;
import org.dromara.sms4j.api.dao.SmsDao;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 短信配置类
 */
@AutoConfiguration(after = {DataRedisAutoConfiguration.class})
public class SmsAutoConfiguration {

    /**
     * 创建 sms4j 使用的短信缓存访问对象。
     *
     * @return 短信缓存访问对象
     */
    @Primary
    @Bean
    public SmsDao smsDao() {
        return new PlusSmsDao();
    }

    /**
     * 异常处理器
     */
    @Bean
    public SmsExceptionHandler smsExceptionHandler() {
        return new SmsExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public SmsNotificationProviderResolver smsNotificationProviderResolver(
        ObjectProvider<SmsSingleAttemptBlendVerifier> verifier) {
        return new Sms4jNotificationProviderResolver(verifier.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public SmsNotifyChannelAdapter smsNotifyChannelAdapter(SmsNotificationProviderResolver providerResolver) {
        return new SmsNotifyChannelAdapter(providerResolver);
    }

}
