package org.namewta.common.nacos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class NacosPropertySourceRegistrarTest {

    @Test
    void keepsDeploymentPropertiesAboveTheRemoteOverlay() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("local-yaml", Map.of("feature.mode", "local")));
        NacosConfigManager manager = new NacosConfigManager(environment, NacosConfigSettings.from(environment));

        NacosPropertySourceRegistrar.register(environment, manager.propertySource());
        manager.apply("feature.mode: remote", NacosUpdateOrigin.STARTUP);
        environment.getPropertySources().addFirst(new MapPropertySource("commandLineArgs", Map.of("feature.mode", "cli")));

        assertThat(environment.getProperty("feature.mode")).isEqualTo("cli");
        environment.getPropertySources().remove("commandLineArgs");
        assertThat(environment.getProperty("feature.mode")).isEqualTo("remote");
    }

    @Test
    void keepsSystemPropertiesAboveTheRemoteOverlayWithoutSystemEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
            StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, Map.of("feature.mode", "system")));
        NacosConfigManager manager = new NacosConfigManager(environment, NacosConfigSettings.from(environment));

        NacosPropertySourceRegistrar.register(environment, manager.propertySource());
        manager.apply("feature.mode: remote", NacosUpdateOrigin.STARTUP);

        assertThat(environment.getProperty("feature.mode")).isEqualTo("system");
    }
}
