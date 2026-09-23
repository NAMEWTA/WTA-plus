package org.namewta.common.notify.idempotency;

import org.namewta.common.notify.model.NotifyResult;

import java.time.Duration;

/**
 * 通知幂等状态存储。
 */
public interface NotifyIdempotencyStore {

    Claim acquire(String storageKey, String digest, String requestId, Duration window);

    void complete(Acquired acquired, NotifyResult result);

    /** 仅当前 owner 可将明确未受理的占位转为可重取，且不得延长原 TTL。 */
    void markRetryable(Acquired acquired);

    void release(Acquired acquired);

    sealed interface Claim permits Acquired, InProgress, Completed, Conflict {
    }

    record Acquired(String storageKey, String digest, String requestId, String expectedValue,
                    Duration window) implements Claim {
    }

    record InProgress(String originalRequestId) implements Claim {
    }

    record Completed(String digest, String originalRequestId, NotifyResult result) implements Claim {
    }

    record Conflict(String originalRequestId) implements Claim {
    }
}
