package org.namewta.third.adapter.resilience;

import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.port.ThirdResiliencePort;
import org.namewta.third.support.ThirdLimitLease;
import org.namewta.third.support.ThirdRejectedException;
import org.redisson.api.RSemaphore;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class ThirdResiliencePolicyAdapter implements ThirdResiliencePort {
    private final RedissonClient redissonClient;

    public ThirdResiliencePolicyAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public ThirdLimitLease acquire(ThirdProvider provider, ThirdEndpoint endpoint) {
        List<RSemaphore> acquired = new ArrayList<>();
        try {
            if (positive(provider.getRateLimit()) && rateLimited("third:rate:provider:" + provider.getProviderCode(), provider.getRateLimit())) {
                throw new ThirdRejectedException(ThirdPartyFailureCategory.RATE_LIMITED, "Provider rate limit exceeded");
            }
            if (positive(endpoint.getRateLimit()) && rateLimited("third:rate:endpoint:" + provider.getProviderCode() + ":" + endpoint.getEndpointCode(), endpoint.getRateLimit())) {
                throw new ThirdRejectedException(ThirdPartyFailureCategory.RATE_LIMITED, "Endpoint rate limit exceeded");
            }
            acquireSemaphore(acquired, "third:concurrency:provider:" + provider.getProviderCode(), provider.getConcurrencyLimit());
            acquireSemaphore(acquired, "third:concurrency:endpoint:" + provider.getProviderCode() + ":" + endpoint.getEndpointCode(), endpoint.getConcurrencyLimit());
            return () -> acquired.forEach(this::release);
        } catch (ThirdRejectedException e) {
            acquired.forEach(this::release);
            throw e;
        } catch (Throwable e) {
            acquired.forEach(this::release);
            throw new ThirdRejectedException(ThirdPartyFailureCategory.CONFIG_UNAVAILABLE, "Third-party limit service unavailable");
        }
    }

    public int maxAttempts(ThirdEndpoint endpoint) {
        return Boolean.TRUE.equals(endpoint.getIdempotent()) ? 1 + Math.min(Math.max(endpoint.getRetryCount() == null ? 0 : endpoint.getRetryCount(), 0), 3) : 1;
    }

    private void acquireSemaphore(List<RSemaphore> acquired, String key, Integer limit) {
        if (!positive(limit)) return;
        RSemaphore semaphore = redissonClient.getSemaphore(key);
        semaphore.trySetPermits(limit);
        if (!semaphore.tryAcquire()) throw new ThirdRejectedException(ThirdPartyFailureCategory.REJECTED, "Concurrency limit exceeded");
        acquired.add(semaphore);
    }

    private boolean rateLimited(String key, int rate) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, rate, Duration.ofSeconds(1), Duration.ZERO);
        return !rateLimiter.tryAcquire();
    }

    private void release(RSemaphore semaphore) {
        try {
            semaphore.release();
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean positive(Integer value) {
        return value != null && value > 0;
    }
}
