package org.namewta.test.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 验证公开本地模板只从外部属性取得凭据，不连接任何外部服务。 */
@Tag("dev")
@ExtendWith(OutputCaptureExtension.class)
class LocalConfigurationUnitTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().getPropertySources()
            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(CredentialProbe.class)
        .withPropertyValues("spring.config.location=" + Path.of("src/main/resources/application-local.example.yml")
            .toAbsolutePath().toUri());

    @Test
    void loadsInjectedCredentialsWithoutLoggingThem(CapturedOutput output) {
        runner.withPropertyValues("DB_URL=jdbc:mysql://127.0.0.1:13306/synthetic",
                "DB_USERNAME=synthetic-user", "DB_PASSWORD=synthetic-db-canary",
                "REDIS_PASSWORD=synthetic-redis-canary")
            .run(context -> {
                assertNull(context.getStartupFailure());
                Credentials credentials = context.getBean(Credentials.class);
                assertEquals("synthetic-db-canary", credentials.databasePassword());
                assertEquals("synthetic-redis-canary", credentials.redisPassword());
                assertEquals("jdbc:mysql://127.0.0.1:13306/synthetic", credentials.url());
                assertEquals("synthetic-user", credentials.username());
            });
        assertFalse(output.getAll().contains("synthetic-db-canary"));
        assertFalse(output.getAll().contains("synthetic-redis-canary"));
    }

    @Test
    void missingDatabasePasswordFailsWithoutAWeakFallback(CapturedOutput output) {
        runner.withPropertyValues("DB_URL=jdbc:mysql://127.0.0.1:13306/synthetic",
                "DB_USERNAME=synthetic-user", "REDIS_PASSWORD=synthetic-redis-canary")
            .run(context -> {
                assertNotNull(context.getStartupFailure());
                assertTrue(rootCause(context.getStartupFailure()).getMessage().contains("DB_PASSWORD"));
            });
        assertFalse(output.getAll().contains("synthetic-redis-canary"));
    }

    @Test
    void missingRedisPasswordFailsWithoutLeakingTheDatabasePassword(CapturedOutput output) {
        runner.withPropertyValues("DB_URL=jdbc:mysql://127.0.0.1:13306/synthetic",
                "DB_USERNAME=synthetic-user", "DB_PASSWORD=synthetic-db-canary")
            .run(context -> {
                assertNotNull(context.getStartupFailure());
                assertTrue(rootCause(context.getStartupFailure()).getMessage().contains("REDIS_PASSWORD"));
            });
        assertFalse(output.getAll().contains("synthetic-db-canary"));
    }

    private static Throwable rootCause(Throwable exception) {
        while (exception.getCause() != null) {
            exception = exception.getCause();
        }
        return exception;
    }

    @Configuration(proxyBeanMethods = false)
    static class CredentialProbe {
        @Bean
        Credentials credentials(Environment environment) {
            return new Credentials(
                environment.getRequiredProperty("spring.datasource.dynamic.datasource.master.url"),
                environment.getRequiredProperty("spring.datasource.dynamic.datasource.master.username"),
                environment.getRequiredProperty("spring.datasource.dynamic.datasource.master.password"),
                environment.getRequiredProperty("spring.data.redis.password"));
        }
    }

    record Credentials(String url, String username, String databasePassword, String redisPassword) { }
}
