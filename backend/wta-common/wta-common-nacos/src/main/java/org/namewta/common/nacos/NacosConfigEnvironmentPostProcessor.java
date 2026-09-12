package org.namewta.common.nacos;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 在 ConfigData 完成后加载可选 Nacos 稀疏覆盖。
 */
public final class NacosConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        NacosConfigSettings settings = NacosConfigSettings.from(environment);
        if (!settings.enabled()) {
            return;
        }
        NacosConfigBootstrap.start(environment, settings, NacosSdkConfigClient::create);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
