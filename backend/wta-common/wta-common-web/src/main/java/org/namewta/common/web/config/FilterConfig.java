package org.namewta.common.web.config;

import jakarta.servlet.DispatcherType;
import org.namewta.common.web.config.properties.XssProperties;
import org.namewta.common.web.config.properties.RequestBodyProperties;
import org.namewta.common.web.filter.RepeatableFilter;
import org.namewta.common.web.filter.XssFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Filter配置
 */
@AutoConfiguration
@EnableConfigurationProperties({XssProperties.class, RequestBodyProperties.class})
public class FilterConfig {

    /**
     * 注册 XSS 过滤器。
     *
     * @param xssProperties XSS 配置
     * @param bodyProperties 未由机器入口捕获正文时使用的字节预算
     * @return XSS 请求过滤器实例
     */
    @Bean
    @ConditionalOnProperty(value = "xss.enabled", havingValue = "true")
    @FilterRegistration(
        name = "xssFilter",
        urlPatterns = "/*",
        order = FilterRegistrationBean.HIGHEST_PRECEDENCE + 4,
        asyncSupported = true,
        dispatcherTypes = DispatcherType.REQUEST
    )
    public XssFilter xssFilter(XssProperties xssProperties, RequestBodyProperties bodyProperties) {
        return new XssFilter(xssProperties, bodyProperties.maxBytes());
    }

    /**
     * 注册可重复读取请求体过滤器。
     *
     * @param bodyProperties 普通 JSON 入口的字节预算
     * @return 请求包装过滤器实例
     */
    @Bean
    @FilterRegistration(
        name = "repeatableFilter",
        urlPatterns = "/*",
        order = FilterRegistrationBean.HIGHEST_PRECEDENCE + 2,
        asyncSupported = true,
        dispatcherTypes = DispatcherType.REQUEST
    )
    public RepeatableFilter repeatableFilter(RequestBodyProperties bodyProperties) {
        return new RepeatableFilter(bodyProperties.maxBytes());
    }

}
