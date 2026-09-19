package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.store.RedisEnterpriseTransferChallengeStore;
import org.namewta.profile.enterprise.domain.transfer.EnterpriseTransferChallenge;
import cn.hutool.crypto.digest.BCrypt;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.StageResult;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.VerificationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "PROFILE_REDIS_E2E_ADDRESS", matches = ".+")
class RedisEnterpriseTransferChallengeStoreE2ETest {

    private final List<String> challengeIds = new ArrayList<>();
    private final List<long[]> rates = new ArrayList<>();
    private RedissonClient client;
    private RedisEnterpriseTransferChallengeStore store;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        var server = config.useSingleServer().setAddress(System.getenv("PROFILE_REDIS_E2E_ADDRESS"));
        String password = System.getenv("PROFILE_REDIS_E2E_PASSWORD");
        if (password != null && !password.isBlank()) {
            server.setPassword(password);
        }
        client = Redisson.create(config);
        store = new RedisEnterpriseTransferChallengeStore(client);
    }

    @AfterEach
    void cleanUp() {
        if (client == null) {
            return;
        }
        challengeIds.forEach(id -> client.getBucket(
            RedisEnterpriseTransferChallengeStore.challengeKey(id)).delete());
        rates.forEach(pair -> client.getBucket(
            RedisEnterpriseTransferChallengeStore.rateKey(pair[0], pair[1])).delete());
        client.shutdown();
    }

    @Test
    void pendingCannotConfirmRateLimitAppliesAndActivationPreservesTtl() {
        EnterpriseTransferChallenge first = challenge("transfer-redis-stage-1", 7101L, 7201L, "123456");
        track(first);
        assertThat(store.stage(first)).isEqualTo(StageResult.STAGED);
        assertThat(store.verify(first.challengeId(), first.sourceUserId(), "123456").status())
            .isEqualTo(VerificationStatus.INVALID);

        EnterpriseTransferChallenge second = challenge("transfer-redis-stage-2", 7101L, 7201L, "123456");
        track(second);
        assertThat(store.stage(second)).isEqualTo(StageResult.RATE_LIMITED);
        assertThat(store.activate(first.challengeId())).isTrue();
        long ttl = client.getBucket(RedisEnterpriseTransferChallengeStore.challengeKey(first.challengeId()))
            .remainTimeToLive();
        assertThat(ttl).isPositive().isLessThanOrEqualTo(300_000L);
    }

    @Test
    void fifthWrongCodeBurnsTheChallenge() {
        EnterpriseTransferChallenge challenge = challenge("transfer-redis-attempts", 7102L, 7202L, "123456");
        track(challenge);
        assertThat(store.stage(challenge)).isEqualTo(StageResult.STAGED);
        assertThat(store.activate(challenge.challengeId())).isTrue();

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(store.verify(challenge.challengeId(), challenge.sourceUserId(), "000000").status())
                .isEqualTo(VerificationStatus.INVALID);
        }
        assertThat(store.verify(challenge.challengeId(), challenge.sourceUserId(), "123456").status())
            .isEqualTo(VerificationStatus.INVALID);
        assertThat(client.getBucket(RedisEnterpriseTransferChallengeStore.challengeKey(challenge.challengeId()))
            .isExists()).isFalse();
    }

    @Test
    void verifiedChallengeCanBeConsumedByOnlyOneConcurrentCaller() throws Exception {
        EnterpriseTransferChallenge challenge = challenge("transfer-redis-consume", 7103L, 7203L, "123456");
        track(challenge);
        assertThat(store.stage(challenge)).isEqualTo(StageResult.STAGED);
        assertThat(store.activate(challenge.challengeId())).isTrue();
        var verification = store.verify(challenge.challengeId(), challenge.sourceUserId(), "123456");
        assertThat(verification.status()).isEqualTo(VerificationStatus.VERIFIED);

        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> consume(start, verification.verified()));
            var second = executor.submit(() -> consume(start, verification.verified()));
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
        assertThat(store.verify(challenge.challengeId(), challenge.sourceUserId(), "123456").status())
            .isEqualTo(VerificationStatus.INVALID);
    }

    private boolean consume(CountDownLatch start,
                            EnterpriseTransferChallengeStore.VerifiedChallenge verified) throws Exception {
        start.await();
        return store.consume(verified);
    }

    @Test
    void repeatedActivationPreservesTheVerificationTokenAndOriginalExpiry() {
        var challenge = challenge("transfer-redis-idempotent", 7104L, 7204L, "123456");
        track(challenge);
        assertThat(store.stage(challenge)).isEqualTo(StageResult.STAGED);
        assertThat(store.activate(challenge.challengeId())).isTrue();
        var verified = store.verify(challenge.challengeId(), challenge.sourceUserId(), "123456");
        long before = client.getBucket(RedisEnterpriseTransferChallengeStore.challengeKey(challenge.challengeId())).remainTimeToLive();
        assertThat(store.activate(challenge.challengeId())).isTrue();
        assertThat(client.getBucket(RedisEnterpriseTransferChallengeStore.challengeKey(challenge.challengeId())).remainTimeToLive())
            .isLessThanOrEqualTo(before);
        assertThat(store.consume(verified.verified())).isTrue();
        var replacement = challenge("transfer-redis-after-consume", 7104L, 7204L, "123456");
        track(replacement);
        assertThat(store.stage(replacement)).isEqualTo(StageResult.STAGED);
    }

    @Test
    void revokingAnOldChallengeCannotClearTheNewChallengesRateLimit() {
        var first = challenge("transfer-redis-revoke-old", 7105L, 7205L, "123456");
        var second = challenge("transfer-redis-revoke-new", 7105L, 7205L, "123456");
        var third = challenge("transfer-redis-revoke-third", 7105L, 7205L, "123456");
        track(first); track(second); track(third);
        assertThat(store.stage(first)).isEqualTo(StageResult.STAGED);
        store.revoke(first.challengeId());
        assertThat(store.stage(second)).isEqualTo(StageResult.STAGED);
        store.revoke(first.challengeId());
        assertThat(store.stage(third)).isEqualTo(StageResult.RATE_LIMITED);
    }

    private EnterpriseTransferChallenge challenge(String challengeId, long sourceUserId,
                                                   long targetUserId, String code) {
        return new EnterpriseTransferChallenge(challengeId, sourceUserId, targetUserId, 7301L, 7401L, 3,
            7501L, "张三", "3001", "13800138000", BCrypt.hashpw(code),
            EnterpriseTransferChallenge.State.PENDING_DELIVERY, 0, System.currentTimeMillis() + 300_000L);
    }

    private void track(EnterpriseTransferChallenge challenge) {
        challengeIds.add(challenge.challengeId());
        rates.add(new long[]{challenge.sourceUserId(), challenge.targetUserId()});
    }
}
