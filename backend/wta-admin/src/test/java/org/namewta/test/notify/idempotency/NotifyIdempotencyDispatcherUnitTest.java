package org.namewta.test.notify.idempotency;

import org.namewta.common.notify.core.NotifyDispatcher;
import org.namewta.common.notify.event.NotifyDeliveryEvent;
import org.namewta.common.notify.exception.NotifyIdempotencyConflictException;
import org.namewta.common.notify.exception.NotifyIdempotencyUnavailableException;
import org.namewta.common.notify.exception.NotifyInProgressException;
import org.namewta.common.notify.exception.NotifyDeliveryException;
import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.idempotency.NotifyIdempotencyCoordinator;
import org.namewta.common.notify.idempotency.NotifyIdempotencyProperties;
import org.namewta.common.notify.idempotency.NotifyIdempotencyStore;
import org.namewta.common.notify.model.*;
import org.namewta.common.notify.registry.NotifyChannelRegistry;
import org.namewta.common.notify.spi.NotifyChannelAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知 Redis 幂等调度合同测试。
 */
@Tag("dev")
class NotifyIdempotencyDispatcherUnitTest {

    @Test
    void shouldReuseCompletedResultAndPublishSkipWithoutDelivery() {
        MemoryStore store = new MemoryStore();
        AtomicInteger providerCalls = new AtomicInteger();
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        NotifyDispatcher dispatcher = dispatcher(store, providerCalls, events, null, null);

        NotifyResult first = dispatcher.send(request("request-1", "order-100", "content"));
        NotifyResult duplicate = dispatcher.send(request("request-2", "order-100", "content"));

        assertEquals(first, duplicate);
        assertEquals(1, providerCalls.get());
        assertEquals(2, events.size());
        NotifyDeliveryEvent skipped = events.get(1);
        assertEquals(NotifyStatus.SKIPPED_DUPLICATE, skipped.result().status());
        assertEquals("request-1", skipped.originalRequestId());
        assertTrue(skipped.result().deliveries().isEmpty());
    }

    @Test
    void shouldRejectConcurrentInProgressImmediately() throws Exception {
        MemoryStore store = new MemoryStore();
        AtomicInteger providerCalls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        NotifyDispatcher dispatcher = dispatcher(store, providerCalls, new ArrayList<>(), entered, release);

        Thread first = Thread.ofVirtual().start(() -> dispatcher.send(request("request-1", "order-100", "content")));
        assertTrue(entered.await(2, TimeUnit.SECONDS));

        NotifyInProgressException exception = assertThrows(NotifyInProgressException.class,
            () -> dispatcher.send(request("request-2", "order-100", "content")));

        assertEquals("request-1", exception.originalRequestId());
        assertEquals(1, providerCalls.get());
        release.countDown();
        first.join();
    }

    @Test
    void shouldRejectSameBusinessKeyWithDifferentDigest() {
        MemoryStore store = new MemoryStore();
        AtomicInteger providerCalls = new AtomicInteger();
        NotifyDispatcher dispatcher = dispatcher(store, providerCalls, new ArrayList<>(), null, null);
        dispatcher.send(request("request-1", "order-100", "content-a"));

        NotifyIdempotencyConflictException exception = assertThrows(NotifyIdempotencyConflictException.class,
            () -> dispatcher.send(request("request-2", "order-100", "content-b")));

        assertEquals("request-1", exception.originalRequestId());
        assertEquals(1, providerCalls.get());
    }

    @Test
    void shouldFailClosedOnlyWhenRequestRequiresIdempotency() {
        AtomicInteger providerCalls = new AtomicInteger();
        NotifyIdempotencyStore unavailable = new NotifyIdempotencyStore() {
            @Override
            public Claim acquire(String storageKey, String digest, String requestId, Duration window) {
                throw new IllegalStateException("redis down");
            }

            @Override
            public void complete(Acquired acquired, NotifyResult result) {
                throw new IllegalStateException("redis down");
            }

            @Override
            public void markRetryable(Acquired acquired) {
                throw new IllegalStateException("redis down");
            }

            @Override
            public void release(Acquired acquired) {
                throw new IllegalStateException("redis down");
            }
        };
        NotifyDispatcher dispatcher = dispatcher(unavailable, providerCalls, new ArrayList<>(), null, null);

        assertThrows(NotifyIdempotencyUnavailableException.class,
            () -> dispatcher.send(request("request-1", "order-100", "content")));
        NotifyResult withoutKey = assertDoesNotThrow(() -> dispatcher.send(NotifyRequest.builder()
            .requestId("request-2")
            .channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.phone("13800000000")))
            .content(new NotifyTextContent("subject", "content"))
            .build()));

        assertEquals(NotifyStatus.ACCEPTED, withoutKey.status());
        assertEquals(1, providerCalls.get());
    }

    @Test
    void shouldUseFiveMinuteDefaultAndHashSensitiveScope() {
        MemoryStore store = new MemoryStore();
        NotifyIdempotencyProperties properties = new NotifyIdempotencyProperties();
        NotifyIdempotencyCoordinator coordinator = new NotifyIdempotencyCoordinator(store, properties);
        NotifyRequest first = request("request-1", "secret-order-key", "content");
        NotifyRequest second = request("request-2", "secret-order-key", "content");

        coordinator.begin(first);

        assertEquals(Duration.ofMinutes(5), store.lastWindow);
        assertEquals(coordinator.digest(first), coordinator.digest(second));
        assertFalse(store.lastStorageKey.contains("secret-order-key"));
        assertFalse(store.lastStorageKey.contains("13800000000"));
        assertEquals("IDEMPOTENCY_WINDOW_OUT_OF_RANGE",
            assertThrows(NotifyValidationException.class,
                () -> coordinator.resolveWindow(Duration.ofSeconds(1))).code());
        assertEquals("IDEMPOTENCY_WINDOW_OUT_OF_RANGE",
            assertThrows(NotifyValidationException.class,
                () -> coordinator.resolveWindow(Duration.ofDays(2))).code());
    }

    @Test
    void shouldReuseFailedResultWithoutCallingProviderAgain() {
        MemoryStore store = new MemoryStore();
        AtomicInteger providerCalls = new AtomicInteger();
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() {
                return NotifyChannel.of("test");
            }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest adapterRequest) {
                providerCalls.incrementAndGet();
                NotifyTarget target = adapterRequest.request().targets().getFirst();
                return new NotifyAdapterResult("provider-a",
                    List.of(NotifyTargetResult.failed(target, "REJECTED", "rejected", 1L)));
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, events::add,
            new NotifyIdempotencyCoordinator(store, new NotifyIdempotencyProperties()));

        NotifyDeliveryException first = assertThrows(NotifyDeliveryException.class,
            () -> dispatcher.send(request("request-1", "order-100", "content")));
        NotifyDeliveryException duplicate = assertThrows(NotifyDeliveryException.class,
            () -> dispatcher.send(request("request-2", "order-100", "content")));

        assertEquals(first.result(), duplicate.result());
        assertEquals(1, providerCalls.get());
        assertEquals(NotifyStatus.SKIPPED_DUPLICATE, events.getLast().result().status());
        assertTrue(events.getLast().result().deliveries().isEmpty());
    }

    @Test
    void explicitUnsentRetryableResultPermitsOneNewPhysicalAttempt() {
        MemoryStore store = new MemoryStore();
        AtomicInteger providerCalls = new AtomicInteger();
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() { return NotifyChannel.of("test"); }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest adapterRequest) {
                int call = providerCalls.incrementAndGet();
                NotifyTarget target = adapterRequest.request().targets().getFirst();
                NotifyTargetResult attempt = call == 1
                    ? new NotifyTargetResult(target, NotifyDeliveryStatus.valueOf("UNSENT_RETRYABLE"),
                        null, "RATE_LIMIT_30S", "Provider 明确未受理", 1L)
                    : NotifyTargetResult.accepted(target, "message-2", 1L);
                return new NotifyAdapterResult("provider-a", List.of(attempt));
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, event -> {},
            new NotifyIdempotencyCoordinator(store, new NotifyIdempotencyProperties()));

        NotifyDeliveryException first = assertThrows(NotifyDeliveryException.class,
            () -> dispatcher.send(request("same-id", "order-100", "content")));
        assertEquals("UNSENT_RETRYABLE", first.result().deliveries().getFirst().status().name());
        NotifyResult accepted = dispatcher.send(request("same-id", "order-100", "content"));
        assertEquals(NotifyStatus.ACCEPTED, accepted.status());
        assertEquals(2, providerCalls.get(), "第二次必须实际进入 Adapter，不能复用第一笔失败缓存");
        assertEquals(accepted, dispatcher.send(request("same-id", "order-100", "content")));
        assertEquals(2, providerCalls.get());
    }

    @Test
    void retryableTransitionFailureKeepsTheProviderOutcomeButNeverReentersProvider() {
        AtomicInteger providerCalls = new AtomicInteger();
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        MemoryStore store = new MemoryStore() {
            @Override
            public synchronized void markRetryable(Acquired acquired) {
                throw new IllegalStateException("redis transition unavailable");
            }
        };
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() { return NotifyChannel.of("test"); }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest adapterRequest) {
                providerCalls.incrementAndGet();
                return new NotifyAdapterResult("provider-a", List.of(
                    NotifyTargetResult.unsentRetryable(adapterRequest.request().targets().getFirst(),
                        "RATE_LIMIT_30S", 1L)));
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, events::add,
            new NotifyIdempotencyCoordinator(store, new NotifyIdempotencyProperties()));

        NotifyIdempotencyUnavailableException failure = assertThrows(NotifyIdempotencyUnavailableException.class,
            () -> dispatcher.send(request("same-id", "order-100", "content")));
        assertEquals("RETRYABLE_TRANSITION", failure.phase());
        assertEquals(NotifyDeliveryStatus.UNSENT_RETRYABLE, events.getFirst().result().deliveries().getFirst().status());
        assertThrows(NotifyInProgressException.class,
            () -> dispatcher.send(request("same-id", "order-100", "content")));
        assertEquals(1, providerCalls.get());
    }

    @Test
    void outcomeUnknownMayRetainProviderIdInternallyButRedactsPublishedEventAndNeverResends() {
        MemoryStore store = new MemoryStore();
        AtomicInteger providerCalls = new AtomicInteger();
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() { return NotifyChannel.of("test"); }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest adapterRequest) {
                providerCalls.incrementAndGet();
                return new NotifyAdapterResult("provider-a", List.of(new NotifyTargetResult(
                    adapterRequest.request().targets().getFirst(), NotifyDeliveryStatus.OUTCOME_UNKNOWN,
                    "canary-provider-id", "PROVIDER_ERROR", "结果待核对", 1L)));
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, events::add,
            new NotifyIdempotencyCoordinator(store, new NotifyIdempotencyProperties()));
        NotifyRequest sensitive = NotifyRequest.builder()
            .requestId("canary-request")
            .bizType("order").bizId("100")
            .channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.phone("13800000000")))
            .content(new NotifyTextContent("subject", "content"))
            .idempotencyKey("order-100")
            .auditPolicy(NotifyAuditPolicy.REDACT_SENSITIVE).build();

        NotifyDeliveryException first = assertThrows(NotifyDeliveryException.class,
            () -> dispatcher.send(sensitive));
        assertEquals("canary-provider-id", first.result().deliveries().getFirst().providerMessageId());
        assertNull(events.getFirst().result().deliveries().getFirst().providerMessageId());
        NotifyDeliveryException duplicate = assertThrows(NotifyDeliveryException.class,
            () -> dispatcher.send(sensitive));
        assertEquals(first.result(), duplicate.result());
        assertEquals(1, providerCalls.get());
        assertFalse(events.toString().contains("canary-provider-id"));
        assertFalse(events.toString().contains("canary-request"));
    }

    @Test
    void oneRetryableTargetCannotReleaseAMixedOrUnknownBatch() {
        MemoryStore store = new MemoryStore();
        AtomicInteger providerCalls = new AtomicInteger();
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() { return NotifyChannel.of("test"); }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest adapterRequest) {
                providerCalls.incrementAndGet();
                List<NotifyTarget> targets = adapterRequest.request().targets();
                return new NotifyAdapterResult("provider-a", List.of(
                    NotifyTargetResult.unsentRetryable(targets.get(0), "RATE_LIMIT_30S", 1L),
                    NotifyTargetResult.outcomeUnknown(targets.get(1), 1L)));
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, event -> {},
            new NotifyIdempotencyCoordinator(store, new NotifyIdempotencyProperties()));
        NotifyRequest request = NotifyRequest.builder()
            .requestId("same-id").bizType("order").bizId("100")
            .channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.phone("13800000000"), NotifyTarget.phone("13900000000")))
            .content(new NotifyTextContent("subject", "content"))
            .idempotencyKey("order-100").build();

        NotifyDeliveryException first = assertThrows(NotifyDeliveryException.class, () -> dispatcher.send(request));
        NotifyDeliveryException duplicate = assertThrows(NotifyDeliveryException.class, () -> dispatcher.send(request));
        assertEquals(first.result(), duplicate.result());
        assertEquals(1, providerCalls.get());
    }

    @Test
    void shouldExposeCompletionFailureAfterPublishingActualProviderResult() {
        AtomicInteger providerCalls = new AtomicInteger();
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        MemoryStore store = new MemoryStore() {
            @Override
            public synchronized void complete(Acquired acquired, NotifyResult result) {
                throw new IllegalStateException("redis write failed");
            }
        };
        NotifyDispatcher dispatcher = dispatcher(store, providerCalls, events, null, null);

        NotifyIdempotencyUnavailableException exception = assertThrows(NotifyIdempotencyUnavailableException.class,
            () -> dispatcher.send(request("request-1", "order-100", "content")));

        assertEquals("COMPLETE", exception.phase());
        assertEquals(1, providerCalls.get());
        assertEquals(1, events.size());
        assertEquals(NotifyStatus.ACCEPTED, events.getFirst().result().status());
    }

    private NotifyDispatcher dispatcher(NotifyIdempotencyStore store, AtomicInteger providerCalls,
                                        List<NotifyDeliveryEvent> events, CountDownLatch entered,
                                        CountDownLatch release) {
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() {
                return NotifyChannel.of("test");
            }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest adapterRequest) {
                providerCalls.incrementAndGet();
                if (entered != null) {
                    entered.countDown();
                }
                if (release != null) {
                    try {
                        assertTrue(release.await(2, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }
                List<NotifyTargetResult> deliveries = adapterRequest.request().targets().stream()
                    .map(target -> NotifyTargetResult.accepted(target, "message-1", 1L))
                    .toList();
                return new NotifyAdapterResult("provider-a", deliveries);
            }
        };
        return new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)), NotifyContext::empty,
            events::add, new NotifyIdempotencyCoordinator(store, new NotifyIdempotencyProperties()));
    }

    private NotifyRequest request(String requestId, String idempotencyKey, String content) {
        return NotifyRequest.builder()
            .requestId(requestId)
            .bizType("order")
            .bizId("100")
            .channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.phone("13800000000")))
            .content(new NotifyTextContent("subject", content))
            .idempotencyKey(idempotencyKey)
            .build();
    }

    private static class MemoryStore implements NotifyIdempotencyStore {

        private Claim current;
        private Duration lastWindow;
        private String lastStorageKey;
        private boolean retryable;
        private int generation;

        @Override
        public synchronized Claim acquire(String storageKey, String digest, String requestId, Duration window) {
            lastStorageKey = storageKey;
            lastWindow = window;
            if (current == null) {
                current = new Acquired(storageKey, digest, requestId, "pending-" + ++generation, window);
                return current;
            }
            String existingDigest = current instanceof Acquired acquired ? acquired.digest()
                : current instanceof Completed completed ? completed.digest() : "";
            String originalRequestId = current instanceof Acquired acquired ? acquired.requestId()
                : current instanceof Completed completed ? completed.originalRequestId() : null;
            if (!existingDigest.equals(digest)) {
                return new Conflict(originalRequestId);
            }
            if (current instanceof Acquired) {
                if (retryable) {
                    retryable = false;
                    current = new Acquired(storageKey, digest, originalRequestId,
                        "pending-" + ++generation, window);
                    return current;
                }
                return new InProgress(originalRequestId);
            }
            return current;
        }

        @Override
        public synchronized void complete(Acquired acquired, NotifyResult result) {
            if (current != acquired) throw new IllegalStateException("stale owner");
            current = new Completed(acquired.digest(), acquired.requestId(), result);
        }

        @Override
        public synchronized void markRetryable(Acquired acquired) {
            if (current != acquired) throw new IllegalStateException("stale owner");
            retryable = true;
        }

        @Override
        public synchronized void release(Acquired acquired) {
            if (current == acquired) {
                current = null;
            }
        }
    }
}
