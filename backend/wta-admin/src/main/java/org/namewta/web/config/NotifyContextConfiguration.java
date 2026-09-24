package org.namewta.web.config;

import cn.dev33.satoken.SaManager;
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
        // Outbox Worker 没有请求上下文；审计身份可为空，附件授权仍取 Intent 的原提交者。
        return new RequestNotifyContextResolver(
            () -> SaManager.getSaTokenContext().isValid() ? LoginHelper.getLoginUser() : null,
            NotifyContextConfiguration::traceId);
    }

    private static String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? MDC.get("trace_id") : traceId;
    }
}
