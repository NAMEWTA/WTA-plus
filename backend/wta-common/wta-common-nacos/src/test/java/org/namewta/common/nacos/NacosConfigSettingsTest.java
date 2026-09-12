package org.namewta.common.nacos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class NacosConfigSettingsTest {

    @Test
    void defaultsToDisabledLocalConfiguration() {
        NacosConfigSettings settings = NacosConfigSettings.from(new MockEnvironment());

        assertThat(settings.enabled()).isFalse();
        assertThat(settings.profile()).isEqualTo("local");
        assertThat(settings.namespace()).isEqualTo("local");
        assertThat(settings.group()).isEqualTo("DEFAULT_GROUP");
        assertThat(settings.dataId()).isEqualTo("wta-namewta.yml");
        assertThat(settings.dataId()).isNotEqualTo("ruoyi-namewta.yml");
        assertThat(NacosConfigConstants.DEFAULT_DATA_ID).isEqualTo("wta-namewta.yml");
    }

    @Test
    void selectsTheNamespaceForTheActiveProfile() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("nacos.config.enabled", "true")
            .withProperty("nacos.config.namespaces.dev", "dev-namespace-id");
        environment.setActiveProfiles("dev");

        NacosConfigSettings settings = NacosConfigSettings.from(environment);

        assertThat(settings.enabled()).isTrue();
        assertThat(settings.profile()).isEqualTo("dev");
        assertThat(settings.namespace()).isEqualTo("dev-namespace-id");
    }
}
