package org.namewta.common.redis.cache;

import org.namewta.third.adapter.resilience.ThirdResiliencePolicyAdapter;
import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.support.ThirdLimitLease;
import org.namewta.third.support.ThirdRejectedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class ThirdRedisIntegrationTest {

    private RedissonClient clientA;
    private RedissonClient clientB;
    private RedissonClient clientC;

    @AfterEach
    void shutdown() {
        if (clientA != null && !clientA.isShutdown()) clientA.shutdown();
        if (clientB != null && !clientB.isShutdown()) clientB.shutdown();
        if (clientC != null && !clientC.isShutdown()) clientC.shutdown();
    }

    @Test
    void twoNodesInvalidateAndRequireEverySubscriberAcknowledgement() {
        RedisSettings settings = settings();
        clientA = client(settings);
        clientB = client(settings);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String namespace = "third:config";
        String key = "third:config:qcc-" + suffix + ":company-basic";
        String channel = "namewta:test:third:invalidation:" + suffix;
        String acknowledgementPrefix = "namewta:test:third:invalidation:ack:" + suffix + ":";
        Map<String, String> cacheA = new ConcurrentHashMap<>();
        Map<String, String> cacheB = new ConcurrentHashMap<>();
        cacheA.put(key, "old-a");
        cacheB.put(key, "old-b");
        JsonMapper mapper = JsonMapper.builder().build();

        try (ClusterCacheInvalidationCoordinator coordinatorA = coordinator(
                 clientA, mapper, "third-a-" + suffix, channel, acknowledgementPrefix);
             ClusterCacheInvalidationCoordinator coordinatorB = coordinator(
                 clientB, mapper, "third-b-" + suffix, channel, acknowledgementPrefix)) {
            coordinatorA.register(namespace, handler(cacheA));
            coordinatorB.register(namespace, handler(cacheB));

            coordinatorA.invalidate(namespace, key);

            assertThat(cacheA).doesNotContainKey(key);
            assertThat(cacheB).doesNotContainKey(key);

            clientC = client(settings);
            RTopic silentSubscriber = clientC.getTopic(channel);
            int listenerId = silentSubscriber.addListener(String.class, (ignoredChannel, payload) -> { });
            try {
                assertThatThrownBy(() -> coordinatorA.clear(namespace))
                    .isInstanceOf(ClusterCacheInvalidationException.class)
                    .hasMessageContaining("acknowledgements timed out");
            } finally {
                silentSubscriber.removeListener(listenerId);
            }
        }
    }

    @Test
    void providerBoundsRateAndConcurrencyAndRedisFailureIsClosed() {
        RedisSettings settings = settings();
        clientA = client(settings);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        ThirdResiliencePolicyAdapter policy = new ThirdResiliencePolicyAdapter(clientA);

        ThirdProvider rateProvider = provider("rate-" + suffix, 1, 0);
        ThirdEndpoint rateEndpoint = endpoint("rate-endpoint", 0, 0);
        try (ThirdLimitLease ignored = policy.acquire(rateProvider, rateEndpoint)) {
            // First request owns the only permit in this one-second window.
        }
        AtomicInteger outboundCalls = new AtomicInteger();
        assertThatThrownBy(() -> {
            try (ThirdLimitLease ignored = policy.acquire(rateProvider, rateEndpoint)) {
                outboundCalls.incrementAndGet();
            }
        }).isInstanceOfSatisfying(ThirdRejectedException.class,
            error -> assertThat(error.category()).isEqualTo(ThirdPartyFailureCategory.RATE_LIMITED));
        assertThat(outboundCalls).hasValue(0);

        ThirdProvider concurrencyProvider = provider("concurrency-" + suffix, 0, 1);
        ThirdEndpoint concurrencyEndpoint = endpoint("concurrency-endpoint", 0, 2);
        ThirdLimitLease first = policy.acquire(concurrencyProvider, concurrencyEndpoint);
        try {
            assertThatThrownBy(() -> policy.acquire(concurrencyProvider, concurrencyEndpoint))
                .isInstanceOfSatisfying(ThirdRejectedException.class,
                    error -> assertThat(error.category()).isEqualTo(ThirdPartyFailureCategory.REJECTED));
        } finally {
            first.close();
        }
        try (ThirdLimitLease ignored = policy.acquire(concurrencyProvider, concurrencyEndpoint)) {
            assertThat(ignored).isNotNull();
        }

        clientA.getKeys().delete(
            "third:rate:provider:" + rateProvider.getProviderCode(),
            "third:rate:endpoint:" + rateProvider.getProviderCode() + ":" + rateEndpoint.getEndpointCode(),
            "third:concurrency:provider:" + concurrencyProvider.getProviderCode(),
            "third:concurrency:endpoint:" + concurrencyProvider.getProviderCode() + ":" + concurrencyEndpoint.getEndpointCode());
        clientA.shutdown();

        assertThatThrownBy(() -> policy.acquire(provider("down-" + suffix, 1, 0), endpoint("down", 0, 0)))
            .isInstanceOfSatisfying(ThirdRejectedException.class,
                error -> assertThat(error.category()).isEqualTo(ThirdPartyFailureCategory.CONFIG_UNAVAILABLE));
    }

    private static ClusterCacheInvalidationCoordinator coordinator(RedissonClient client, JsonMapper mapper,
                                                                    String nodeId, String channel,
                                                                    String acknowledgementPrefix) {
        return new ClusterCacheInvalidationCoordinator(
            new RedissonCacheInvalidationTransport(client, mapper, channel, acknowledgementPrefix),
            Duration.ofMillis(500), nodeId);
    }

    private static CacheInvalidationHandler handler(Map<String, String> cache) {
        return new CacheInvalidationHandler() {
            @Override
            public void invalidate(String keyFingerprint) {
                cache.keySet().removeIf(key ->
                    ClusterCacheInvalidationCoordinator.fingerprint(key).equals(keyFingerprint));
            }

            @Override
            public void clear() {
                cache.clear();
            }
        };
    }

    private static ThirdProvider provider(String code, int rateLimit, int concurrencyLimit) {
        ThirdProvider provider = new ThirdProvider();
        provider.setProviderCode(code);
        provider.setRateLimit(rateLimit);
        provider.setConcurrencyLimit(concurrencyLimit);
        return provider;
    }

    private static ThirdEndpoint endpoint(String code, int rateLimit, int concurrencyLimit) {
        ThirdEndpoint endpoint = new ThirdEndpoint();
        endpoint.setEndpointCode(code);
        endpoint.setRateLimit(rateLimit);
        endpoint.setConcurrencyLimit(concurrencyLimit);
        return endpoint;
    }

    private static RedisSettings settings() {
        int port = Integer.parseInt(setting("third.redis.integration.port", "THIRD_REDIS_INTEGRATION_PORT", "-1"));
        Assumptions.assumeTrue(port > 0, "requires a disposable Redis port");
        return new RedisSettings(
            setting("third.redis.integration.host", "THIRD_REDIS_INTEGRATION_HOST", "127.0.0.1"),
            port,
            setting("third.redis.integration.password", "THIRD_REDIS_INTEGRATION_PASSWORD", ""));
    }

    private static RedissonClient client(RedisSettings settings) {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        var server = config.useSingleServer().setAddress("redis://" + settings.host() + ":" + settings.port());
        if (!settings.password().isBlank()) server.setPassword(settings.password());
        return Redisson.create(config);
    }

    private static String setting(String property, String environment, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        if (value == null || value.isBlank()) return defaultValue;
        String normalized = value.trim();
        if (normalized.length() >= 2
            && ((normalized.startsWith("\"") && normalized.endsWith("\""))
            || (normalized.startsWith("'") && normalized.endsWith("'")))) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private record RedisSettings(String host, int port, String password) { }
}
