package org.namewta.third.adapter.store;

import com.baomidou.dynamic.datasource.tx.TransactionContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.common.redis.cache.CacheInvalidationHandler;
import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.port.ThirdConfigSnapshot;
import org.namewta.third.port.ThirdConfigSnapshotPort;
import org.namewta.third.port.ThirdEndpointConfigStore;
import org.namewta.third.port.ThirdProviderConfigStore;
import org.namewta.third.port.ThirdResiliencePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ThirdConfigCacheAdapter implements ThirdConfigSnapshotPort {
    private static final String PREFIX = "third:config:";
    private static final String INVALIDATION_NAMESPACE = "third:config";
    private final ThirdProviderConfigStore providerStore;
    private final ThirdEndpointConfigStore endpointStore;
    private final ClusterCacheInvalidationCoordinator invalidationCoordinator;
    private final ThirdResiliencePort resilience;
    private final Map<String, String> knownKeys = new ConcurrentHashMap<>();
    private AutoCloseable invalidationRegistration;

    public ThirdConfigCacheAdapter(ThirdProviderConfigStore providerStore,
                                   ThirdEndpointConfigStore endpointStore,
                                   ClusterCacheInvalidationCoordinator invalidationCoordinator,
                                   ThirdResiliencePort resilience) {
        this.providerStore = providerStore;
        this.endpointStore = endpointStore;
        this.invalidationCoordinator = invalidationCoordinator;
        this.resilience = resilience;
    }

    @PostConstruct
    void registerInvalidationHandler() {
        invalidationRegistration = invalidationCoordinator.register(INVALIDATION_NAMESPACE,
            new CacheInvalidationHandler() {
                @Override
                public void invalidate(String keyFingerprint) {
                    knownKeys.remove(keyFingerprint);
                }

                @Override
                public void clear() {
                    knownKeys.clear();
                }
            });
    }

    @PreDestroy
    void unregisterInvalidationHandler() {
        if (invalidationRegistration == null) return;
        try {
            invalidationRegistration.close();
        } catch (Exception ignored) {
            // Coordinator owns the subscription lifecycle.
        }
    }

    public ThirdConfigSnapshot get(String providerCode, String endpointCode) {
        String key = key(providerCode, endpointCode);
        try {
            String cached = RedisUtils.getCacheObject(key);
            if (cached != null && !cached.isBlank()) {
                ThirdConfigSnapshot snapshot = JsonUtils.parseObject(cached, ThirdConfigSnapshot.class);
                if (snapshot != null && snapshot.getProvider() != null && snapshot.getEndpoint() != null) {
                    remember(key);
                    return snapshot;
                }
            }
        } catch (Throwable e) {
            throw unavailable(e);
        }
        ThirdProvider provider = providerStore.findActiveByCode(providerCode);
        ThirdEndpoint endpoint = provider == null ? null : endpointStore.findActiveByProviderAndCode(provider.getProviderId(), endpointCode);
        if (provider == null || endpoint == null || !provider.getProviderId().equals(endpoint.getProviderId())) {
            throw new ServiceException("第三方接口配置不存在");
        }
        ThirdConfigSnapshot snapshot = new ThirdConfigSnapshot(provider, endpoint);
        try {
            RedisUtils.setCacheObject(key, JsonUtils.toJsonString(snapshot), Duration.ofMinutes(10));
            remember(key);
        } catch (Throwable e) {
            throw unavailable(e);
        }
        return snapshot;
    }

    public void evict(String providerCode, String endpointCode) {
        if (TransactionContext.getXID() != null) {
            TransactionContext.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { evictCommitted(providerCode, endpointCode); }
            });
        } else {
            evictCommitted(providerCode, endpointCode);
        }
    }

    private void evictCommitted(String providerCode, String endpointCode) {
        try {
            if (providerCode != null && endpointCode != null) {
                RedisUtils.deleteObject(key(providerCode, endpointCode));
                forget(key(providerCode, endpointCode));
                invalidationCoordinator.invalidate(INVALIDATION_NAMESPACE, key(providerCode, endpointCode));
            } else if (providerCode != null) {
                endpointStore.findAllByProviderCode(providerCode).forEach(endpoint ->
                    evictLocalKey(key(providerCode, endpoint.getEndpointCode())));
                invalidationCoordinator.clear(INVALIDATION_NAMESPACE);
            } else {
                knownKeys.clear();
                invalidationCoordinator.clear(INVALIDATION_NAMESPACE);
            }
            // Read committed versions, not the mutable pre-save entity. Requests also repair a failed Redis update.
            if (providerCode != null) {
                ThirdProvider provider = providerStore.findActiveByCode(providerCode);
                if (provider != null) {
                    ThirdEndpoint endpoint = endpointCode == null ? null
                        : endpointStore.findActiveByProviderAndCode(provider.getProviderId(), endpointCode);
                    resilience.refresh(provider, endpoint);
                }
            }
        } catch (RuntimeException e) {
            throw unavailable(e);
        }
    }

    public static String key(String providerCode, String endpointCode) {
        return PREFIX + providerCode + ":" + endpointCode;
    }

    private void evictLocalKey(String key) {
        RedisUtils.deleteObject(key);
        forget(key);
    }

    private void remember(String key) {
        knownKeys.put(ClusterCacheInvalidationCoordinator.fingerprint(key), key);
    }

    private void forget(String key) {
        knownKeys.remove(ClusterCacheInvalidationCoordinator.fingerprint(key));
    }

    private static ServiceException unavailable(Throwable cause) {
        return new ServiceException("第三方配置缓存不可用");
    }
}
