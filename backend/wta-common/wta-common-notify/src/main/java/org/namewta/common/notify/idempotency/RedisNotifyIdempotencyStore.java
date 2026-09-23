package org.namewta.common.notify.idempotency;

import org.namewta.common.notify.model.NotifyResult;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.api.bucket.CompareAndDeleteArgs;
import org.redisson.api.bucket.CompareAndSetArgs;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 使用单个 Redis Bucket 完成原子占位和完成态 CAS。
 */
public final class RedisNotifyIdempotencyStore implements NotifyIdempotencyStore {

    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final String RETRYABLE = "RETRYABLE";
    private static final String CAS_KEEP_TTL = """
        if redis.call('get', KEYS[1]) ~= ARGV[1] then return 0 end
        if redis.call('pttl', KEYS[1]) <= 0 then return 0 end
        redis.call('set', KEYS[1], ARGV[2], 'KEEPTTL')
        return 1
        """;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final RedissonClient redissonClient;

    public RedisNotifyIdempotencyStore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Claim acquire(String storageKey, String digest, String requestId, Duration window) {
        RBucket<String> bucket = redissonClient.getBucket(storageKey);
        String pendingValue = pending(digest, requestId);
        for (int attempt = 0; attempt < 3; attempt++) {
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
            if (RETRYABLE.equals(current.state()) && current.result() == null
                && current.ownerNonce() != null && !current.ownerNonce().isBlank()) {
                // 新 owner 必须有独立 nonce；旧 owner 的 expectedValue 再也不能等于新占位。
                String replacement = pending(digest, current.requestId());
                if (replaceKeepingTtl(bucket, currentValue, replacement)) {
                    return new Acquired(storageKey, digest, current.requestId(), replacement, window);
                }
                continue;
            }
            throw new IllegalStateException("Redis 通知幂等状态无效");
        }
        throw new IllegalStateException("Redis 通知幂等状态在竞争中消失");
    }

    @Override
    public void complete(Acquired acquired, NotifyResult result) {
        RBucket<String> bucket = redissonClient.getBucket(acquired.storageKey());
        String completedValue = JSON_MAPPER.writeValueAsString(
            new StoredState(COMPLETED, acquired.digest(), acquired.requestId(), result, null));
        CompareAndSetArgs<String> args = CompareAndSetArgs.expected(acquired.expectedValue())
            .set(completedValue)
            .timeToLive(acquired.window());
        if (!bucket.compareAndSet(args)) {
            throw new IllegalStateException("Redis 通知幂等占位已失效");
        }
    }

    @Override
    public void markRetryable(Acquired acquired) {
        RBucket<String> bucket = redissonClient.getBucket(acquired.storageKey());
        StoredState current = JSON_MAPPER.readValue(acquired.expectedValue(), StoredState.class);
        if (!IN_PROGRESS.equals(current.state()) || current.ownerNonce() == null) {
            throw new IllegalStateException("Redis 通知幂等 owner 无效");
        }
        String retryableValue = JSON_MAPPER.writeValueAsString(
            new StoredState(RETRYABLE, acquired.digest(), acquired.requestId(), null, current.ownerNonce()));
        if (!replaceKeepingTtl(bucket, acquired.expectedValue(), retryableValue)) {
            throw new IllegalStateException("Redis 通知幂等占位已失效");
        }
    }

    @Override
    public void release(Acquired acquired) {
        RBucket<String> bucket = redissonClient.getBucket(acquired.storageKey());
        bucket.compareAndDelete(CompareAndDeleteArgs.expected(acquired.expectedValue()));
    }

    private String pending(String digest, String requestId) {
        return JSON_MAPPER.writeValueAsString(
            new StoredState(IN_PROGRESS, digest, requestId, null, UUID.randomUUID().toString()));
    }

    /** 脚本采用 bucket 的实际 codec，兼容生产 CompositeCodec 与测试 StringCodec；KEEPTTL 不延长期限。 */
    private boolean replaceKeepingTtl(RBucket<String> bucket, String expected, String replacement) {
        Long changed = redissonClient.getScript(bucket.getCodec()).eval(bucket.getName(), RScript.Mode.READ_WRITE,
            CAS_KEEP_TTL, RScript.ReturnType.LONG, List.of(bucket.getName()), expected, replacement);
        return Long.valueOf(1L).equals(changed);
    }

    /** 旧四字段 IN_PROGRESS/COMPLETED JSON 的缺失 ownerNonce 按 null 读取，不能升级为可重试。 */
    public record StoredState(String state, String digest, String requestId, NotifyResult result,
                              String ownerNonce) {
    }
}
