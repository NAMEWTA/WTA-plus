package org.namewta.sso.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.sso.api.SsoRuntimeSettings;
import org.namewta.sso.service.SsoConfigurationService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@Tag("dev")
class SsoCookieConfigurationTest {
    @Test
    void databaseSnapshotOwnsCookieAndYamlCannotOverrideIt() {
        runner(true)
                .withPropertyValues("namewta.sso.cookie-secure=false")
                .run(
                        c -> {
                            assertThat(c).hasNotFailed();
                            assertThat(c.getBean(SsoProperties.class).isCookieSecure()).isTrue();
                        });
    }

    @Test
    void httpExceptionsRequireExplicitDevelopmentProfiles() {
        for (String[] profiles :
                new String[][] {{}, {"prod"}, {"dev", "prod"}, {"test"}, {"local", "staging"}})
            runner(false)
                    .withInitializer(c -> c.getEnvironment().setActiveProfiles(profiles))
                    .run(c -> assertThat(c).hasFailed());
        for (String[] profiles : new String[][] {{"local"}, {"dev"}, {"local", "dev"}})
            runner(false)
                    .withInitializer(c -> c.getEnvironment().setActiveProfiles(profiles))
                    .run(
                            c -> {
                                assertThat(c).hasNotFailed();
                                assertThat(c.getBean(SsoProperties.class).isCookieSecure())
                                        .isFalse();
                            });
    }

    private ApplicationContextRunner runner(boolean secure) {
        return new ApplicationContextRunner()
                .withUserConfiguration(SsoAutoConfiguration.class)
                .withBean(
                        SsoConfigurationService.class,
                        () -> {
                            var service = mock(SsoConfigurationService.class);
                            when(service.current())
                                    .thenReturn(
                                            new SsoRuntimeSettings(
                                                    true,
                                                    "https://sso.example",
                                                    "/",
                                                    "Sso-Token",
                                                    secure,
                                                    300,
                                                    28800));
                            return service;
                        });
    }
}
