package org.namewta.common.notify.config;

import org.namewta.common.notify.attachment.NotifyAttachmentSnapshotService;
import org.namewta.common.notify.core.NotifyClient;
import org.namewta.common.notify.core.NotifyDispatcher;
import org.namewta.common.notify.event.NotifyEventPublisher;
import org.namewta.common.notify.idempotency.NotifyIdempotencyCoordinator;
import org.namewta.common.notify.idempotency.NotifyIdempotencyProperties;
import org.namewta.common.notify.idempotency.NotifyIdempotencyStore;
import org.namewta.common.notify.idempotency.RedisNotifyIdempotencyStore;
import org.namewta.common.notify.model.NotifyContext;
import org.namewta.common.notify.registry.NotifyChannelRegistry;
import org.namewta.common.notify.spi.NotifyChannelAdapter;
import org.namewta.common.notify.spi.NotifyContextResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.redisson.api.RedissonClient;

import java.util.List;

/**
 * 统一通知自动装配。
 */
@AutoConfiguration
@EnableConfigurationProperties(NotifyIdempotencyProperties.class)
public class NotifyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotifyContextResolver notifyContextResolver() {
        return NotifyContext::empty;
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyEventPublisher notifyEventPublisher(ApplicationEventPublisher publisher) {
        return publisher::publishEvent;
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyChannelRegistry notifyChannelRegistry(ObjectProvider<NotifyChannelAdapter> adapters) {
        List<NotifyChannelAdapter> adapterList = adapters.orderedStream().toList();
        return new NotifyChannelRegistry(adapterList);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(NotifyIdempotencyStore.class)
    public NotifyIdempotencyStore notifyIdempotencyStore(RedissonClient redissonClient) {
        return new RedisNotifyIdempotencyStore(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyIdempotencyCoordinator notifyIdempotencyCoordinator(
        ObjectProvider<NotifyIdempotencyStore> store,
        NotifyIdempotencyProperties properties) {
        return new NotifyIdempotencyCoordinator(store.getIfAvailable(), properties);
    }

    @Bean
    @ConditionalOnMissingBean(NotifyClient.class)
    public NotifyClient notifyClient(NotifyChannelRegistry registry, NotifyContextResolver contextResolver,
                                     NotifyEventPublisher eventPublisher,
                                     NotifyIdempotencyCoordinator idempotencyCoordinator,
                                     ObjectProvider<NotifyAttachmentSnapshotService> attachmentSnapshotService) {
        return new NotifyDispatcher(registry, contextResolver, eventPublisher, idempotencyCoordinator,
            attachmentSnapshotService.getIfAvailable());
    }
}
