package org.namewta.test.sso;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.json.enhance.JsonValueEnhancer;
import org.namewta.common.web.config.ResourcesConfig;
import org.namewta.common.web.config.properties.CorsProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.filter.CorsFilter;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** 以真实 Spring ConfigData 和配置 Bean 验证各 profile 的 CORS 白名单。 */
@Tag("dev")
class SsoCorsProfileBindingTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withConfiguration(AutoConfigurations.of(ResourcesConfig.class))
        .withBean(JsonValueEnhancer.class, () -> mock(JsonValueEnhancer.class));

    @Test
    void productionDefaultsToNoCrossOriginPermission() {
        production().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CorsFilter.class);
            assertThat(context.getEnvironment().getActiveProfiles()).contains("prod");
            assertThat(context.getBean(CorsProperties.class).validatedOrigins()).isEmpty();
        });
    }

    @Test
    void productionBindsOnlyExplicitOrigins() {
        production().withPropertyValues(
            "WEB_CORS_ALLOWED_ORIGINS=https://admin.example.test,https://sso.example.test")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(CorsFilter.class);
                assertThat(context.getBean(CorsProperties.class).validatedOrigins())
                    .containsExactly("https://admin.example.test", "https://sso.example.test");
            });
    }

    @Test
    void productionWildcardWithCredentialsFailsAtStartup() {
        production().withPropertyValues("WEB_CORS_ALLOWED_ORIGINS=*")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(rootCause(context.getStartupFailure()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CORS requires exact HTTP(S) origins without wildcards");
            });
    }

    @Test
    void localExampleBindsOnlyCurrentViteProxyOriginsAndCanBeOverridden() {
        var local = runner.withPropertyValues("spring.profiles.active=local",
            "spring.config.location=" + Path.of("src/main/resources/application-local.example.yml")
                .toAbsolutePath().toUri());
        local.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CorsFilter.class);
            assertThat(context.getBean(CorsProperties.class).validatedOrigins())
                .containsExactly("http://127.0.0.1:5177", "http://127.0.0.1:5175", "http://127.0.0.1:4176");
        });
        local.withPropertyValues("WEB_CORS_ALLOWED_ORIGINS=http://127.0.0.1:6177")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(CorsProperties.class).validatedOrigins())
                    .containsExactly("http://127.0.0.1:6177");
            });
    }

    private ApplicationContextRunner production() {
        return runner.withPropertyValues("spring.profiles.active=prod",
            "spring.config.location=classpath:/application.yml");
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
