package org.namewta.web.config;

import org.namewta.common.notify.spi.NotifyContextResolver;
import org.namewta.common.satoken.utils.LoginHelper;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为统一通知装配平台请求上下文；Client 仅作为认证请求来源快照。
 */
@Configuration(proxyBeanMethods = false)
public class NotifyContextConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotifyContextResolver.class)
    public NotifyContextResolver requestNotifyContextResolver() {
        return new RequestNotifyContextResolver(LoginHelper::getLoginUser, NotifyContextConfiguration::traceId);
    }

    private static String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? MDC.get("trace_id") : traceId;
    }
}
