package org.namewta.common.web.config;

import jakarta.servlet.DispatcherType;
import org.namewta.common.web.logging.SysLogEventWriter;
import org.namewta.common.web.logging.SysLogFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/**
 * 系统 HTTP 日志自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(SysLogProperties.class)
public class SysLogConfig {

    /**
     * 在 CryptoFilter 解密和 repeatable 包装之后、XSS 改写之前记录 Filter 所见数据。
     *
     * @param properties 系统日志配置
     * @param jsonMapper 项目统一 JSON 映射器
     * @return 系统 HTTP 日志过滤器
     */
    @Bean
    @ConditionalOnProperty(prefix = "sys.log", name = "enabled", havingValue = "true", matchIfMissing = true)
    @FilterRegistration(
        name = "sysLogFilter",
        urlPatterns = "/*",
        order = FilterRegistrationBean.HIGHEST_PRECEDENCE + 2,
        asyncSupported = true,
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.ASYNC}
    )
    public SysLogFilter sysLogFilter(SysLogProperties properties, JsonMapper jsonMapper) {
        return new SysLogFilter(properties.maxBodyBytes(), new SysLogEventWriter(jsonMapper));
    }
}
