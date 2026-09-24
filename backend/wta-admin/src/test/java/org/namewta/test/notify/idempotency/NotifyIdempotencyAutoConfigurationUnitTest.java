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
import org.redisson.spring.starter.RedissonAutoConfigurationV4;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void shouldDiscoverRedissonFromActualAutoConfigurationOrder() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotifyAutoConfiguration.class,
                RedissonAutoConfigurationV4.class))
            .withInitializer(context -> context.addBeanFactoryPostProcessor(beanFactory -> {
                // 条件装配已经完成后才替换物理客户端，不能提前注册用户 Bean 掩盖顺序缺陷。
                for (String name : beanFactory.getBeanDefinitionNames()) {
                    beanFactory.getBeanDefinition(name).setLazyInit(true);
                }
                AbstractBeanDefinition redisson = (AbstractBeanDefinition) beanFactory.getBeanDefinition("redisson");
                redisson.setInstanceSupplier(() -> mock(RedissonClient.class));
            }))
            .run(context -> {
                assertThat(context.containsBeanDefinition("notifyIdempotencyStore"))
                    .as("真实 Redisson 自动配置之后必须注册生产幂等存储").isTrue();
                assertInstanceOf(RedisNotifyIdempotencyStore.class, context.getBean(NotifyIdempotencyStore.class));
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
