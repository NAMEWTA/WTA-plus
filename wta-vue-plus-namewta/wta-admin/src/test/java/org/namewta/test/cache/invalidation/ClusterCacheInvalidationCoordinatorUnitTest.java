package org.namewta.test.cache.invalidation;

import org.namewta.common.redis.cache.CacheInvalidationHandler;
import org.namewta.common.redis.cache.CacheInvalidationMessage;
import org.namewta.common.redis.cache.CacheInvalidationTransport;
import org.namewta.common.redis.cache.ClusterCacheInvalidationCoordinator;
import org.namewta.common.redis.cache.ClusterCacheInvalidationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ClusterCacheInvalidationCoordinatorUnitTest {

    @Test
    void shouldInvalidateEverySubscribedNodeWithoutPublishingTheRawKey() {
        InMemoryTransport transport = new InMemoryTransport();
        Map<String, String> nodeA = new ConcurrentHashMap<>();
        Map<String, String> nodeB = new ConcurrentHashMap<>();
        String sensitiveKey = "satoken:login:token-value-that-must-not-leak";
        nodeA.put(sensitiveKey, "cached-a");
        nodeB.put(sensitiveKey, "cached-b");

        try (ClusterCacheInvalidationCoordinator coordinatorA = coordinator(transport, "node-a");
             ClusterCacheInvalidationCoordinator coordinatorB = coordinator(transport, "node-b")) {
            coordinatorA.register("sa-token", handler(nodeA));
            coordinatorB.register("sa-token", handler(nodeB));

            coordinatorA.invalidate("sa-token", sensitiveKey);

            assertFalse(nodeA.containsKey(sensitiveKey));
            assertFalse(nodeB.containsKey(sensitiveKey));
            assertFalse(transport.lastMessage.toString().contains(sensitiveKey));
            assertTrue(transport.lastMessage.keyFingerprint().matches("[0-9a-f]{64}"));
        }
    }

    @Test
    void shouldExposeMissingAcknowledgementsToTheCaller() {
        InMemoryTransport transport = new InMemoryTransport();
        try (ClusterCacheInvalidationCoordinator coordinator = coordinator(transport, "node-a")) {
            coordinator.register("spring:user", new CacheInvalidationHandler() {
                @Override
                public void invalidate(String keyFingerprint) {
                    throw new IllegalStateException("local cache unavailable");
                }

                @Override
                public void clear() {
                    throw new IllegalStateException("local cache unavailable");
                }
            });

            assertThrows(ClusterCacheInvalidationException.class,
                () -> coordinator.invalidate("spring:user", "user-1"));
        }
    }

    @Test
    void shouldClearNamespacesAndRemainIdempotentForDuplicateMessages() {
        InMemoryTransport transport = new InMemoryTransport();
        transport.duplicateDeliveries = true;
        Map<String, String> nodeA = new ConcurrentHashMap<>();
        Map<String, String> nodeB = new ConcurrentHashMap<>();
        nodeA.put("one", "a");
        nodeB.put("two", "b");

        try (ClusterCacheInvalidationCoordinator coordinatorA = coordinator(transport, "node-a");
             ClusterCacheInvalidationCoordinator coordinatorB = coordinator(transport, "node-b")) {
            coordinatorA.register("spring:users", handler(nodeA));
            coordinatorB.register("spring:users", handler(nodeB));

            coordinatorA.clear("spring:users");

            assertTrue(nodeA.isEmpty());
            assertTrue(nodeB.isEmpty());
            assertNull(transport.lastMessage.keyFingerprint());
        }
    }

    @Test
    void shouldExposePublishFailureAndClosedSubscription() {
        InMemoryTransport transport = new InMemoryTransport();
        ClusterCacheInvalidationCoordinator coordinator = coordinator(transport, "node-a");
        coordinator.register("sa-token", handler(new ConcurrentHashMap<>()));
        transport.publishFailure = new IllegalStateException("redis unavailable");

        ClusterCacheInvalidationException publishFailure = assertThrows(ClusterCacheInvalidationException.class,
            () -> coordinator.invalidate("sa-token", "key"));
        assertTrue(publishFailure.getCause() instanceof IllegalStateException);

        transport.publishFailure = null;
        coordinator.close();
        assertThrows(ClusterCacheInvalidationException.class,
            () -> coordinator.invalidate("sa-token", "key"));
    }

    private static ClusterCacheInvalidationCoordinator coordinator(InMemoryTransport transport, String nodeId) {
        return new ClusterCacheInvalidationCoordinator(transport, Duration.ofMillis(20), nodeId);
    }

    private static CacheInvalidationHandler handler(Map<String, String> cache) {
        return new CacheInvalidationHandler() {
            @Override
            public void invalidate(String keyFingerprint) {
                cache.keySet().removeIf(key -> ClusterCacheInvalidationCoordinator.fingerprint(key).equals(keyFingerprint));
            }

            @Override
            public void clear() {
                cache.clear();
            }
        };
    }

    private static final class InMemoryTransport implements CacheInvalidationTransport {
        private final List<Consumer<CacheInvalidationMessage>> listeners = new CopyOnWriteArrayList<>();
        private final Map<String, Set<String>> acknowledgements = new ConcurrentHashMap<>();
        private volatile CacheInvalidationMessage lastMessage;
        private volatile RuntimeException publishFailure;
        private volatile boolean duplicateDeliveries;

        @Override
        public Subscription subscribe(Consumer<CacheInvalidationMessage> listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        @Override
        public long publish(CacheInvalidationMessage message) {
            if (publishFailure != null) {
                throw publishFailure;
            }
            lastMessage = message;
            listeners.forEach(listener -> {
                try {
                    listener.accept(message);
                    if (duplicateDeliveries) {
                        listener.accept(message);
                    }
                } catch (RuntimeException ignored) {
                    // A failed subscriber deliberately withholds its acknowledgement.
                }
            });
            return listeners.size();
        }

        @Override
        public void acknowledge(String requestId, String nodeId) {
            acknowledgements.computeIfAbsent(requestId, ignored -> ConcurrentHashMap.newKeySet()).add(nodeId);
        }

        @Override
        public boolean awaitAcknowledgements(String requestId, long expected, Duration timeout) {
            return acknowledgements.getOrDefault(requestId, Set.of()).size() >= expected;
        }

        @Override
        public void clearAcknowledgements(String requestId) {
            acknowledgements.remove(requestId);
        }
    }
}
