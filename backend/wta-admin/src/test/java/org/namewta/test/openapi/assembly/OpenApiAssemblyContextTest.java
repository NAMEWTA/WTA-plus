package org.namewta.test.openapi.assembly;

import cn.dev33.satoken.stp.StpLogic;
import org.namewta.common.openapi.config.OpenApiAutoConfiguration;
import org.namewta.common.openapi.gateway.OpenApiGatewayFilter;
import org.namewta.common.openapi.nonce.OpenApiNonceStore;
import org.namewta.common.openapi.protocol.OpenApiHeaders;
import org.namewta.common.openapi.ratelimit.OpenApiRateLimiter;
import org.namewta.common.openapi.registry.OpenApiOperationRegistry;
import org.namewta.common.openapi.session.OpenApiMachineSessionBridge;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.common.openapi.session.OpenApiMachineSessionOperations;
import org.namewta.common.openapi.spi.OpenApiAuthorizationResolver;
import org.namewta.common.openapi.spi.OpenApiCallEventPublisher;
import org.namewta.common.openapi.spi.OpenApiCredential;
import org.namewta.common.openapi.spi.OpenApiCredentialResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@Tag("dev")
class OpenApiAssemblyContextTest {

    private static final String VALID_KEK = Base64.getEncoder().encodeToString(new byte[32]);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OpenApiAutoConfiguration.class));

    @Test
    void remainsCompletelyUnassembledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(OpenApiGatewayFilter.class);
            assertThat(context).doesNotHaveBean(OpenApiNonceStore.class);
            assertThat(context).doesNotHaveBean(OpenApiOperationRegistry.class);
            assertThat(context).doesNotHaveBean("openApiGatewayFilterRegistration");
        });
    }

    @Test
    void leavesOrdinaryMvcRequestsUntouchedWhenDisabled() {
        new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class, OpenApiAutoConfiguration.class))
            .withUserConfiguration(OrdinaryEndpointConfiguration.class)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean("openApiGatewayFilterRegistration");
                webAppContextSetup(context).build().perform(get("/ordinary")
                        .header(OpenApiHeaders.VERSION, "invalid")
                        .header(OpenApiHeaders.APP_KEY, "invalid")
                        .header(OpenApiHeaders.TIMESTAMP, "invalid")
                        .header(OpenApiHeaders.NONCE, "invalid")
                        .header(OpenApiHeaders.SIGNATURE, "invalid"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("ok"));
            });
    }

    @Test
    void failsClosedWhenKekIsMissingOrInvalidWithoutEchoingKeyMaterial() {
        dependencies(runner)
            .withPropertyValues("openapi.enabled=true", "openapi.kek-version=v1")
            .run(context -> assertFailureContains(context.getStartupFailure(), "openapi.kek"));

        dependencies(runner)
            .withPropertyValues("openapi.enabled=true", "openapi.kek-version=v1", "openapi.kek=not-a-key")
            .run(context -> {
                assertFailureContains(context.getStartupFailure(), "openapi.kek");
                assertThat(failureText(context.getStartupFailure())).doesNotContain("not-a-key");
            });
    }

    @Test
    void failsClosedWhenSecuritySettingsAreInvalid() {
        dependencies(runner)
            .withPropertyValues("openapi.enabled=true", "openapi.kek-version=invalid version",
                "openapi.kek=" + VALID_KEK)
            .run(context -> assertFailureContains(context.getStartupFailure(), "openapi.kek-version"));

        dependencies(runner)
            .withPropertyValues(validProperties())
            .withPropertyValues("openapi.nonce-ttl=0s")
            .run(context -> assertFailureContains(context.getStartupFailure(), "openapi.nonce-ttl"));

        dependencies(runner)
            .withPropertyValues(validProperties())
            .withPropertyValues("openapi.app-rate-limit-per-minute=0")
            .run(context -> assertFailureContains(context.getStartupFailure(),
                "openapi.app-rate-limit-per-minute"));
    }

    @Test
    void failsClosedWhenRedisOrCredentialSpiIsMissing() {
        dependenciesWithoutRedis(runner)
            .withPropertyValues(validProperties())
            .run(context -> assertFailureContains(context.getStartupFailure(), "RedissonClient"));

        dependenciesWithoutCredential(runner)
            .withPropertyValues(validProperties())
            .run(context -> assertFailureContains(context.getStartupFailure(), "OpenApiCredentialResolver"));
    }

    @Test
    void failsClosedWhenCredentialSpiIsAmbiguous() {
        dependencies(runner)
            .withBean("secondCredentialResolver", OpenApiCredentialResolver.class, OpenApiAssemblyContextTest::credentialResolver)
            .withPropertyValues(validProperties())
            .run(context -> assertFailureContains(context.getStartupFailure(), "OpenApiCredentialResolver"));
    }

    @Test
    void assemblesExactlyOneCompleteGatewayWhenEnabled() {
        dependencies(runner)
            .withPropertyValues(validProperties())
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(OpenApiNonceStore.class);
                assertThat(context).hasSingleBean(OpenApiRateLimiter.class);
                assertThat(context).hasSingleBean(OpenApiMachineSessionOperations.class);
                assertThat(context).hasSingleBean(OpenApiMachineSessionBridge.class);
                assertThat(context).hasSingleBean(OpenApiMachineSessionInvalidator.class);
                assertThat(context).hasSingleBean(OpenApiOperationRegistry.class);
                assertThat(context).hasSingleBean(OpenApiCallEventPublisher.class);
                FilterRegistrationBean<?> registration = context.getBean(
                    "openApiGatewayFilterRegistration", FilterRegistrationBean.class);
                assertThat(registration.getFilter()).isInstanceOf(OpenApiGatewayFilter.class);
                assertThat(registration.getUrlPatterns()).containsExactly("/*");
                assertThat(registration.isAsyncSupported()).isTrue();
            });
    }

    @Test
    void assemblesWhenActuatorContributesAnotherHandlerMapping() {
        RequestMappingHandlerMapping managementMapping = mock(RequestMappingHandlerMapping.class);
        when(managementMapping.getHandlerMethods()).thenReturn(Map.of());

        new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class, OpenApiAutoConfiguration.class))
            .withBean("controllerEndpointHandlerMapping", RequestMappingHandlerMapping.class,
                () -> managementMapping)
            .withBean(StpLogic.class, () -> mock(StpLogic.class))
            .withBean(OpenApiAuthorizationResolver.class, () -> userId -> null)
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .withBean(OpenApiCredentialResolver.class, OpenApiAssemblyContextTest::credentialResolver)
            .withPropertyValues(validProperties())
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(OpenApiOperationRegistry.class);
                assertThat(context).hasBean("openApiGatewayFilterRegistration");
            });
    }

    private static ApplicationContextRunner dependencies(ApplicationContextRunner context) {
        return dependenciesWithoutCredential(context)
            .withBean(OpenApiCredentialResolver.class, OpenApiAssemblyContextTest::credentialResolver);
    }

    private static ApplicationContextRunner dependenciesWithoutCredential(ApplicationContextRunner context) {
        return dependenciesWithoutRedis(context)
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class));
    }

    private static ApplicationContextRunner dependenciesWithoutRedis(ApplicationContextRunner context) {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        when(mapping.getHandlerMethods()).thenReturn(Map.of());
        return context
            .withBean(RequestMappingHandlerMapping.class, () -> mapping)
            .withBean(StpLogic.class, () -> mock(StpLogic.class))
            .withBean(OpenApiAuthorizationResolver.class, () -> userId -> null);
    }

    private static OpenApiCredentialResolver credentialResolver() {
        return appKey -> new OpenApiCredential(1L, 2L, appKey,
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", Instant.now().plusSeconds(60));
    }

    private static String[] validProperties() {
        return new String[] {
            "openapi.enabled=true",
            "openapi.kek-version=v1",
            "openapi.kek=" + VALID_KEK
        };
    }

    private static void assertFailureContains(Throwable failure, String expected) {
        assertThat(failure).isNotNull();
        assertThat(failureText(failure)).contains(expected);
    }

    private static String failureText(Throwable failure) {
        StringBuilder text = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            text.append(current.getClass().getName()).append(':').append(current.getMessage()).append('\n');
        }
        return text.toString();
    }

    @Configuration(proxyBeanMethods = false)
    static class OrdinaryEndpointConfiguration {

        @Bean
        OrdinaryEndpoint ordinaryEndpoint() {
            return new OrdinaryEndpoint();
        }
    }

    @RestController
    static class OrdinaryEndpoint {

        @GetMapping(value = "/ordinary", produces = MediaType.TEXT_PLAIN_VALUE)
        String ordinary() {
            return "ok";
        }
    }
}
