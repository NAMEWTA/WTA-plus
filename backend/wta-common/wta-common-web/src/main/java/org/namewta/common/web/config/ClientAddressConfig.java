package org.namewta.common.web.config;

import jakarta.servlet.DispatcherType;
import org.namewta.common.core.utils.ip.ClientAddressResolver;
import org.namewta.common.web.config.properties.ClientAddressProperties;
import org.namewta.common.web.filter.ClientAddressFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/** 保留真实socket peer，并注册统一来源解析入口。 */
@AutoConfiguration
@EnableConfigurationProperties(ClientAddressProperties.class)
public class ClientAddressConfig {
    /**
     * 容器必须关闭forwarded-header地址重写，否则无法判定真实代理边界。
     *
     * @param properties 显式可信代理配置
     * @param environment 容器转发策略
     * @return 来源地址过滤器
     */
    @Bean
    @FilterRegistration(name = "clientAddressFilter", urlPatterns = "/*",
        order = FilterRegistrationBean.HIGHEST_PRECEDENCE, dispatcherTypes = DispatcherType.REQUEST)
    public ClientAddressFilter clientAddressFilter(ClientAddressProperties properties, Environment environment) {
        if (!"none".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy", "none"))) {
            throw new IllegalArgumentException("Client address resolution requires server.forward-headers-strategy=none");
        }
        return new ClientAddressFilter(new ClientAddressResolver(properties.getTrustedProxies()));
    }
}
