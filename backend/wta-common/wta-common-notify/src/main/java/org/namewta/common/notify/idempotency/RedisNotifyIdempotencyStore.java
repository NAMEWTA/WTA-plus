package org.namewta.common.notify.idempotency;

import org.namewta.common.notify.model.NotifyResult;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.bucket.CompareAndDeleteArgs;
import org.redisson.api.bucket.CompareAndSetArgs;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * 使用单个 Redis Bucket 完成原子占位和完成态 CAS。
 */
public final class RedisNotifyIdempotencyStore implements NotifyIdempotencyStore {

    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final RedissonClient redissonClient;

    public RedisNotifyIdempotencyStore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Claim acquire(String storageKey, String digest, String requestId, Duration window) {
        RBucket<String> bucket = redissonClient.getBucket(storageKey);
        String pendingValue = JSON_MAPPER.writeValueAsString(new StoredState(IN_PROGRESS, digest, requestId, null));
        for (int attempt = 0; attempt < 2; attempt++) {
            if (bucket.setIfAbsent(pendingValue, window)) {
                return new Acquired(storageKey, digest, requestId, pendingValue, window);
            }
            String currentValue = bucket.get();
            if (currentValue == null) {
                continue;
            }
            StoredState current = JSON_MAPPER.readValue(currentValue, StoredState.class);
            if (!digest.equals(current.digest())) {
                return new Conflict(current.requestId());
            }
            if (IN_PROGRESS.equals(current.state())) {
                return new InProgress(current.requestId());
            }
            if (COMPLETED.equals(current.state()) && current.result() != null) {
                return new Completed(current.digest(), current.requestId(), current.result());
            }
            throw new IllegalStateException("Redis 通知幂等状态无效");
        }
        throw new IllegalStateException("Redis 通知幂等状态在竞争中消失");
    }

    @Override
    public void complete(Acquired acquired, NotifyResult result) {
        RBucket<String> bucket = redissonClient.getBucket(acquired.storageKey());
        String completedValue = JSON_MAPPER.writeValueAsString(
            new StoredState(COMPLETED, acquired.digest(), acquired.requestId(), result));
        CompareAndSetArgs<String> args = CompareAndSetArgs.expected(acquired.expectedValue())
            .set(completedValue)
            .timeToLive(acquired.window());
        if (!bucket.compareAndSet(args)) {
            throw new IllegalStateException("Redis 通知幂等占位已失效");
        }
    }

    @Override
    public void release(Acquired acquired) {
        RBucket<String> bucket = redissonClient.getBucket(acquired.storageKey());
        bucket.compareAndDelete(CompareAndDeleteArgs.expected(acquired.expectedValue()));
    }

    public record StoredState(String state, String digest, String requestId, NotifyResult result) {
    }
}
