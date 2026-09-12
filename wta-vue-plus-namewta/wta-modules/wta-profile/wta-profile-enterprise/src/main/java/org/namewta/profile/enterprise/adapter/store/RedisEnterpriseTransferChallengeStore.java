package org.namewta.profile.enterprise.adapter.store;
import org.namewta.profile.enterprise.domain.exception.EnterpriseTransferException;
import org.namewta.profile.enterprise.domain.transfer.EnterpriseTransferChallenge;
import cn.hutool.crypto.digest.BCrypt;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.StageResult;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.Verification;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.VerificationStatus;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.VerifiedChallenge;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;
import org.namewta.common.json.utils.JsonUtils;
import java.time.Duration;
import java.util.UUID;
/** 基于 Redis 的企业转移挑战存储，实现 TTL、限流和分布式锁约束。 */
@Component
public class RedisEnterpriseTransferChallengeStore implements EnterpriseTransferChallengeStore {
    private static final String CHALLENGE_PREFIX = "profile:enterprise:transfer:challenge:";
    private static final String RATE_PREFIX = "profile:enterprise:transfer:rate:";
    private static final String LOCK_PREFIX = "profile:enterprise:transfer:lock:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_TTL = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private final RedissonClient client;
    /** 创建企业转移挑战 Redis 存储。 */
    public RedisEnterpriseTransferChallengeStore(RedissonClient client) {
        this.client = client;
    }
    /** 暂存转移挑战并返回暂存结果。 */
    @Override
    public StageResult stage(EnterpriseTransferChallenge challenge) {
        String rateKey = rateKey(challenge.sourceUserId(), challenge.targetUserId());
        RLock lock = client.getLock(LOCK_PREFIX + rateKey);
        lock.lock();
        try {
            RBucket<String> rate = client.getBucket(rateKey, StringCodec.INSTANCE);
            if (!rate.setIfAbsent(challenge.challengeId(), RATE_TTL)) {
                return StageResult.RATE_LIMITED;
            }
            RBucket<String> bucket = bucket(challenge.challengeId());
            if (!bucket.setIfAbsent(write(new StoredChallenge(challenge, null)), CHALLENGE_TTL)) {
                rate.delete();
                throw new EnterpriseTransferException("ENTERPRISE_TRANSFER_CHALLENGE_CONFLICT");
            }
            return StageResult.STAGED;
        } finally {
            unlock(lock);
        }
    }
    /** 激活转移挑战。 */
    @Override
    public boolean activate(String challengeId) {
        return locked(challengeId, () -> {
            StoredChallenge stored = read(challengeId);
            if (stored == null || stored.challenge().state() != EnterpriseTransferChallenge.State.PENDING_DELIVERY) {
                return false;
            }
            save(challengeId, new StoredChallenge(stored.challenge().activate(), null));
            return true;
        });
    }
    /** 核验转移挑战并返回验证结果。 */
    @Override
    public Verification verify(String challengeId, long sourceUserId, String code) {
        return locked(challengeId, () -> {
            StoredChallenge stored = read(challengeId);
            if (stored == null || stored.challenge().state() != EnterpriseTransferChallenge.State.ACTIVE
                || stored.challenge().sourceUserId() != sourceUserId
                || stored.challenge().expiresAtEpochMilli() <= System.currentTimeMillis()) {
                return invalid();
            }
            if (!BCrypt.checkpw(code, stored.challenge().codeHash())) {
                EnterpriseTransferChallenge attempted = stored.challenge().failedAttempt();
                if (attempted.attempts() >= MAX_ATTEMPTS) {
                    bucket(challengeId).delete();
                } else {
                    save(challengeId, new StoredChallenge(attempted, null));
                }
                return invalid();
            }
            String token = stored.storageToken() == null ? UUID.randomUUID().toString() : stored.storageToken();
            StoredChallenge verified = new StoredChallenge(stored.challenge(), token);
            save(challengeId, verified);
            return new Verification(VerificationStatus.VERIFIED,
                new VerifiedChallenge(verified.challenge(), token));
        });
    }
    /** 消费并移除已验证的转移挑战。 */
    @Override
    public boolean consume(VerifiedChallenge verifiedChallenge) {
        String challengeId = verifiedChallenge.challenge().challengeId();
        return locked(challengeId, () -> {
            StoredChallenge stored = read(challengeId);
            if (stored == null || stored.storageToken() == null
                || !stored.storageToken().equals(verifiedChallenge.storageToken())) {
                return false;
            }
            return bucket(challengeId).delete();
        });
    }
    /** 撤销转移挑战或档案绑定。 */
    @Override
    public void revoke(String challengeId) {
        locked(challengeId, () -> bucket(challengeId).delete());
    }
    /** 保存转移挑战数据。 */
    private void save(String challengeId, StoredChallenge stored) {
        long remaining = bucket(challengeId).remainTimeToLive();
        if (remaining <= 0) {
            bucket(challengeId).delete();
            return;
        }
        bucket(challengeId).set(write(stored), Duration.ofMillis(remaining));
    }
    /** 读取转移挑战存储数据。 */
    private StoredChallenge read(String challengeId) {
        String value = bucket(challengeId).get();
        return value == null ? null : JsonUtils.parseObject(value, StoredChallenge.class);
    }
    /** 写入转移挑战存储数据。 */
    private String write(StoredChallenge stored) {
        return JsonUtils.toJsonString(stored);
    }
    /** 获取转移挑战 Redis 存储桶。 */
    private RBucket<String> bucket(String challengeId) {
        return client.getBucket(challengeKey(challengeId), StringCodec.INSTANCE);
    }
    /** 在分布式锁保护下执行挑战操作。 */
    private <T> T locked(String challengeId, java.util.function.Supplier<T> action) {
        RLock lock = client.getLock(LOCK_PREFIX + challengeId);
        lock.lock();
        try {
            return action.get();
        } finally {
            unlock(lock);
        }
    }
    /** 释放转移挑战分布式锁。 */
    private void unlock(RLock lock) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
    /** 返回无效挑战结果。 */
    private Verification invalid() {
        return new Verification(VerificationStatus.INVALID, null);
    }
    /** 生成挑战 Redis 键。 */
    public static String challengeKey(String challengeId) {
        return CHALLENGE_PREFIX + challengeId;
    }
    /** 生成验证码限流 Redis 键。 */
    public static String rateKey(long sourceUserId, long targetUserId) {
        return RATE_PREFIX + sourceUserId + ":" + targetUserId;
    }
    /** Redis 中保存的企业转移挑战载荷。 */
    private record StoredChallenge(EnterpriseTransferChallenge challenge, String storageToken) {
    }
}
