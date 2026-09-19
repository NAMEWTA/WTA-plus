package org.namewta.third.adapter.resilience;

import lombok.extern.slf4j.Slf4j;
import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.port.ThirdResiliencePort;
import org.namewta.third.support.ThirdLimitLease;
import org.namewta.third.support.ThirdRejectedException;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.redisson.api.ratelimiter.RateLimiterArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared limits on stable keys; configuration changes retain in-flight owners and rate usage. */
@Slf4j
@Component
public class ThirdResiliencePolicyAdapter implements ThirdResiliencePort {
    private final RedissonClient redissonClient;

    public ThirdResiliencePolicyAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public ThirdLimitLease acquire(ThirdProvider provider, ThirdEndpoint endpoint) {
        List<Permit> acquired = new ArrayList<>();
        try {
            long leaseMs = leaseMillis(provider, endpoint);
            dimension("provider:" + provider.getProviderCode(), provider.getVersion(), provider.getRateLimit(),
                provider.getConcurrencyLimit(), leaseMs, acquired);
            dimension("endpoint:" + provider.getProviderCode() + ":" + endpoint.getEndpointCode(), endpoint.getVersion(),
                endpoint.getRateLimit(), endpoint.getConcurrencyLimit(), leaseMs, acquired);
            // Nested acquisition may take time. Never start HTTP with an expired first permit.
            for (Permit permit : acquired) {
                if (!permit.semaphore().updateLeaseTime(permit.id(), leaseMs, TimeUnit.MILLISECONDS)) {
                    throw new ThirdRejectedException(ThirdPartyFailureCategory.REJECTED, "Concurrency permit expired before dispatch");
                }
            }
            AtomicBoolean closed = new AtomicBoolean();
            return () -> { if (closed.compareAndSet(false, true)) acquired.forEach(this::release); };
        } catch (InterruptedException e) {
            acquired.forEach(this::release);
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (RuntimeException e) {
            acquired.forEach(this::release);
            if (e instanceof ThirdRejectedException rejection) throw rejection;
            throw unavailable(e);
        }
    }

    @Override
    public void refresh(ThirdProvider provider, ThirdEndpoint endpoint) {
        try {
            dimension("provider:" + provider.getProviderCode(), provider.getVersion(), provider.getRateLimit(),
                provider.getConcurrencyLimit(), 0, null);
            if (endpoint != null) {
                dimension("endpoint:" + provider.getProviderCode() + ":" + endpoint.getEndpointCode(), endpoint.getVersion(),
                    endpoint.getRateLimit(), endpoint.getConcurrencyLimit(), 0, null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (RuntimeException e) {
            if (e instanceof ThirdRejectedException rejection) throw rejection;
            throw unavailable(e);
        }
    }

    @Override
    public int maxAttempts(ThirdEndpoint endpoint) {
        return Boolean.TRUE.equals(endpoint.getIdempotent())
            ? 1 + Math.min(Math.max(endpoint.getRetryCount() == null ? 0 : endpoint.getRetryCount(), 0), 3) : 1;
    }

    long leaseMillis(ThirdProvider provider, ThirdEndpoint endpoint) {
        long connect = Math.max(100, provider.getTimeoutConnectMs() == null ? 3000 : provider.getTimeoutConnectMs());
        long read = Math.max(100, provider.getTimeoutReadMs() == null ? 10000 : provider.getTimeoutReadMs());
        return (connect + read) * maxAttempts(endpoint) + 1000;
    }

    private void dimension(String key, Integer version, Integer rate, Integer concurrency,
                           long leaseMs, List<Permit> acquired) throws InterruptedException {
        RLock lock = redissonClient.getLock("third:limit-config-lock:" + key);
        if (!lock.tryLock(1, TimeUnit.SECONDS)) {
            throw new ThirdRejectedException(ThirdPartyFailureCategory.CONFIG_UNAVAILABLE, "Limit configuration is busy");
        }
        try {
            RBucket<String> config = redissonClient.getBucket("third:limit-config:" + key, StringCodec.INSTANCE);
            int requestedVersion = version == null ? 0 : version;
            int requestedRate = rate == null ? 0 : rate;
            int requestedConcurrency = concurrency == null ? 0 : concurrency;
            String desired = requestedVersion + ":" + requestedRate + ":" + requestedConcurrency;
            String stored = config.get();
            String[] previous = stored == null ? null : stored.split(":");
            int previousVersion = previous == null ? -1 : Integer.parseInt(previous[0]);
            if (previous != null && (previousVersion > requestedVersion
                || previousVersion == requestedVersion && !stored.startsWith(desired + ":"))) {
                throw new ThirdRejectedException(ThirdPartyFailureCategory.CONFIG_UNAVAILABLE, "Limit configuration snapshot is stale");
            }
            RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore("third:concurrency:" + key);
            var limiter = redissonClient.getRateLimiter("third:rate:" + key);
            long rateNotBefore = previous == null ? 0 : Long.parseLong(previous[3]);
            if (previous == null || previousVersion != requestedVersion || !"ready".equals(previous[4])) {
                if (previous != null && previousVersion != requestedVersion
                    && requestedRate > 0 && requestedRate < Integer.parseInt(previous[1])) {
                    // keepState alone may release excess old-window usage after a downward change.
                    // Drain that one-second window before admitting at the lower rate.
                    rateNotBefore = System.currentTimeMillis() + 1000;
                }
                String revision = desired + ":" + rateNotBefore;
                // An interrupted update leaves a pending revision; retrying it repairs the same stable keys.
                config.set(revision + ":pending");
                semaphore.setPermits(requestedConcurrency > 0 ? requestedConcurrency : Integer.MAX_VALUE);
                if (requestedRate > 0) {
                    limiter.setRate(RateLimiterArgs.of(RateType.OVERALL, requestedRate, Duration.ofSeconds(1)).keepState(true));
                }
                config.set(revision + ":ready");
            }
            if (acquired == null) return;
            if (requestedRate > 0 && (System.currentTimeMillis() < rateNotBefore || !limiter.tryAcquire())) {
                throw new ThirdRejectedException(ThirdPartyFailureCategory.RATE_LIMITED, "Rate limit exceeded");
            }
            // Unlimited calls are tracked too, so a later reduction counts already running requests.
            String permitId = semaphore.tryAcquire(0, leaseMs, TimeUnit.MILLISECONDS);
            if (permitId == null) throw new ThirdRejectedException(ThirdPartyFailureCategory.REJECTED, "Concurrency limit exceeded");
            acquired.add(new Permit(semaphore, permitId));
        } finally {
            lock.unlock();
        }
    }

    private void release(Permit permit) {
        try {
            permit.semaphore().tryRelease(permit.id());
        } catch (RuntimeException e) {
            log.debug("Third permit release deferred to expiry ({})", e.getClass().getSimpleName());
        }
    }

    private static ThirdRejectedException unavailable(Exception cause) {
        return new ThirdRejectedException(ThirdPartyFailureCategory.CONFIG_UNAVAILABLE,
            "Third-party limit service unavailable", cause);
    }

    private record Permit(RPermitExpirableSemaphore semaphore, String id) { }
}
