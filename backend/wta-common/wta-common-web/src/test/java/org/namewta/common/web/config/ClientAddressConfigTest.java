package org.namewta.common.web.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.web.config.properties.ClientAddressProperties;
import org.namewta.common.web.filter.ClientAddressFilter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class ClientAddressConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ClientAddressConfig.class));

    @Test
    void defaultAndEmptyEnvironmentTrustNoProxy() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ClientAddressFilter.class);
            assertThat(context.getBean(ClientAddressProperties.class).getTrustedProxies()).isEmpty();
        });
        runner.withPropertyValues("namewta.web.client-address.trusted-proxies=").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ClientAddressProperties.class).getTrustedProxies()).isEmpty();
        });
    }

    @Test
    void bindsExplicitCidrsAndRejectsMalformedTrustOrContainerRewriting() {
        runner.withPropertyValues("namewta.web.client-address.trusted-proxies=10.0.0.0/24,2001:db8::/64")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(ClientAddressProperties.class).getTrustedProxies())
                    .containsExactly("10.0.0.0/24", "2001:db8::/64");
            });
        runner.withPropertyValues("namewta.web.client-address.trusted-proxies=localhost/32")
            .run(context -> assertThat(context).hasFailed());
        for (String strategy : new String[]{"native", "framework"}) {
            runner.withPropertyValues("server.forward-headers-strategy=" + strategy)
                .run(context -> assertThat(context).hasFailed());
        }
    }
}
