package org.namewta.common.nacos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class NacosConfigAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(NacosConfigAutoConfiguration.class));

    @AfterEach
    void tearDown() {
        NacosConfigBootstrap.shutdown();
    }

    @Test
    void disabledByDefaultCreatesNoRuntimeBeans() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(NacosConfigManager.class);
            assertThat(context).doesNotHaveBean(NacosConfigLifecycle.class);
            assertThat(context).doesNotHaveBean(NacosConfigInfoContributor.class);
        });
    }
}
