package org.namewta.sso.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class SsoCookieConfigurationTest {
    @Test
    void defaultsToSecureAndRefusesHttpExceptionsOutsideExplicitDevelopmentProfiles() {
        new ApplicationContextRunner().withUserConfiguration(SsoAutoConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(SsoProperties.class).isCookieSecure()).isTrue();
        });
        for (String[] profiles : new String[][]{{}, {"prod"}, {"dev", "prod"}, {"test"}, {"local", "staging"}}) {
            httpException(profiles).run(context -> assertThat(context).hasFailed());
        }
        for (String[] profiles : new String[][]{{"local"}, {"dev"}, {"local", "dev"}}) {
            httpException(profiles).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(SsoProperties.class).isCookieSecure()).isFalse();
            });
        }
    }

    private ApplicationContextRunner httpException(String[] profiles) {
        return new ApplicationContextRunner().withUserConfiguration(SsoAutoConfiguration.class)
            .withInitializer(context -> context.getEnvironment().setActiveProfiles(profiles))
            .withPropertyValues("namewta.sso.cookie-secure=false");
    }
}
