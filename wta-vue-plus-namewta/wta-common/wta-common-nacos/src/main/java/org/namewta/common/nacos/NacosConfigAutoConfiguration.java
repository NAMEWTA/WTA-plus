package org.namewta.common.nacos;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

@AutoConfiguration
@EnableConfigurationProperties(NacosConfigProperties.class)
@ConditionalOnProperty(prefix = "nacos.config", name = "enabled", havingValue = "true")
public class NacosConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NacosConfigManager nacosConfigManager(ConfigurableEnvironment environment) {
        NacosConfigBootstrap.Session session = NacosConfigBootstrap.current(environment);
        if (session == null) {
            session = NacosConfigBootstrap.start(environment, NacosConfigSettings.from(environment),
                NacosSdkConfigClient::create);
        }
        return session.manager();
    }

    @Bean
    public NacosConfigLifecycle nacosConfigLifecycle(NacosConfigManager manager,
                                                     ObjectProvider<NacosConfigParticipant<?>> participants) {
        return new NacosConfigLifecycle(manager, participants);
    }

    @Bean
    public InfoContributor nacosConfigInfoContributor(NacosConfigManager manager) {
        return new NacosConfigInfoContributor(manager);
    }
}
