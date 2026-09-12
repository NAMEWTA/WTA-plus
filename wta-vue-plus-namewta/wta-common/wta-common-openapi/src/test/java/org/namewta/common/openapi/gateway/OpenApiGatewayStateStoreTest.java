package org.namewta.common.openapi.gateway;

import org.namewta.common.openapi.nonce.RedissonOpenApiNonceStore;
import org.namewta.common.openapi.ratelimit.RedissonOpenApiRateLimiter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class OpenApiGatewayStateStoreTest {

    @Test
    void nonceRegistrationIsAtomicAndHashesCredentialMaterialInTheRedisKey() {
        RedissonClient client = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<String> bucket = mock(RBucket.class);
        when(client.<String>getBucket(anyString())).thenReturn(bucket);
        when(bucket.setIfAbsent("1", Duration.ofSeconds(60))).thenReturn(true, false);
        RedissonOpenApiNonceStore store = new RedissonOpenApiNonceStore(client);

        assertThat(store.register("public-app-key", "request-nonce", Duration.ofSeconds(60))).isTrue();
        assertThat(store.register("public-app-key", "request-nonce", Duration.ofSeconds(60))).isFalse();

        var key = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(client, org.mockito.Mockito.times(2)).getBucket(key.capture());
        assertThat(key.getAllValues()).allSatisfy(value -> assertThat(value)
            .startsWith("openapi:nonce:")
            .doesNotContain("public-app-key", "request-nonce"));
    }

    @Test
    void rateLimitUsesRedisAtomicLimiterAndFailsClosedOnInfrastructureErrors() {
        RedissonClient client = mock(RedissonClient.class);
        RRateLimiter limiter = mock(RRateLimiter.class);
        when(client.getRateLimiter(anyString())).thenReturn(limiter);
        when(limiter.tryAcquire()).thenReturn(true, false);
        RedissonOpenApiRateLimiter rateLimiter = new RedissonOpenApiRateLimiter(client);

        assertThat(rateLimiter.acquire("app:public-app-key", 10, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiter.acquire("app:public-app-key", 10, Duration.ofMinutes(1))).isFalse();
        verify(limiter, org.mockito.Mockito.times(2))
            .trySetRate(RateType.OVERALL, 10, Duration.ofMinutes(1));

        when(limiter.tryAcquire()).thenThrow(new IllegalStateException("redis-down"));
        assertThatThrownBy(() -> rateLimiter.acquire("app:public-app-key", 11, Duration.ofMinutes(1)))
            .isInstanceOf(OpenApiStateStoreException.class)
            .hasMessage("OpenAPI state store unavailable")
            .hasCauseInstanceOf(IllegalStateException.class);

        var keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(client, org.mockito.Mockito.times(3)).getRateLimiter(keys.capture());
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
        assertThat(keys.getAllValues().get(2)).isNotEqualTo(keys.getAllValues().get(0));
    }
}
