package org.namewta.test.notify.idempotency;

import org.namewta.common.notify.config.NotifyAutoConfiguration;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.exception.NotifyIdempotencyUnavailableException;
import org.namewta.common.notify.idempotency.NotifyIdempotencyCoordinator;
import org.namewta.common.notify.idempotency.NotifyIdempotencyStore;
import org.namewta.common.notify.idempotency.RedisNotifyIdempotencyStore;
import org.namewta.common.notify.model.NotifyChannel;
import org.namewta.common.notify.model.NotifyRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * 通知幂等自动装配边界测试。
 */
@Tag("dev")
class NotifyIdempotencyAutoConfigurationUnitTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(NotifyAutoConfiguration.class));

    @Test
    void shouldKeepNotifyClientAvailableWithoutRedissonAndFailClosedOnlyForKeyedRequests() {
        contextRunner.run(context -> {
            assertNotNull(context.getBean(NotifyClient.class));
            NotifyIdempotencyCoordinator coordinator = context.getBean(NotifyIdempotencyCoordinator.class);
            assertThrows(NotifyIdempotencyUnavailableException.class,
                () -> coordinator.begin(NotifyRequest.builder().channel(NotifyChannel.SMS)
                    .idempotencyKey("order-1").build()));
        });
    }

    @Test
    void shouldCreateRedisStoreWhenRedissonClientExists() {
        contextRunner.withUserConfiguration(RedissonConfiguration.class).run(context -> {
            NotifyIdempotencyStore store = context.getBean(NotifyIdempotencyStore.class);
            assertInstanceOf(RedisNotifyIdempotencyStore.class, store);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class RedissonConfiguration {

        @Bean
        RedissonClient redissonClient() {
            return mock(RedissonClient.class);
        }
    }
}
