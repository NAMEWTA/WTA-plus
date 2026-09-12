package org.namewta.common.redis.cache;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Cluster transport boundary for cache invalidation and acknowledgements.
 */
public interface CacheInvalidationTransport {

    Subscription subscribe(Consumer<CacheInvalidationMessage> listener);

    long publish(CacheInvalidationMessage message);

    void acknowledge(String requestId, String nodeId);

    boolean awaitAcknowledgements(String requestId, long expected, Duration timeout);

    void clearAcknowledgements(String requestId);

    /**
     * Active transport subscription.
     */
    interface Subscription extends AutoCloseable {

        @Override
        void close();
    }
}
