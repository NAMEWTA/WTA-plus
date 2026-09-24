package org.namewta.test.notify.attachment;

import org.namewta.common.notify.attachment.*;
import org.namewta.common.notify.core.NotifyDispatcher;
import org.namewta.common.notify.event.NotifyDeliveryEvent;
import org.namewta.common.notify.exception.NotifyAttachmentSnapshotException;
import org.namewta.common.notify.model.*;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.notify.registry.NotifyChannelRegistry;
import org.namewta.common.notify.spi.NotifyChannelAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dispatcher 附件快照顺序、去重和失败语义测试。
 */
@Tag("dev")
class NotifyAttachmentDispatcherUnitTest {

    @Test
    void shouldCopyUniqueSourcesBeforeProviderAndPublishSnapshotIds() {
        RecordingSnapshotService snapshots = new RecordingSnapshotService(false);
        RecordingAdapter adapter = new RecordingAdapter(false);
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        NotifyDispatcher dispatcher = dispatcher(adapter, snapshots, events);

        NotifyResult result = dispatcher.send(request(List.of(10L, 10L, 20L)));

        assertEquals(NotifyStatus.ACCEPTED, result.status());
        assertEquals(List.of(10L, 20L), snapshots.sources);
        assertEquals(7001L, snapshots.notifyLogId);
        assertEquals(List.of(1010L, 1020L), adapter.snapshotIds);
        assertEquals(7001L, events.getFirst().attachmentOwnerIntentId());
        assertEquals(List.of(1010L, 1020L), events.getFirst().attachmentSnapshotOssIds());
        assertEquals(0, snapshots.cleanupCalls.get());
    }

    @Test
    void snapshotFailureMustReleaseRequestBeforeProvider() {
        RecordingSnapshotService snapshots = new RecordingSnapshotService(true);
        RecordingAdapter adapter = new RecordingAdapter(false);
        NotifyDispatcher dispatcher = dispatcher(adapter, snapshots, new ArrayList<>());

        NotifyAttachmentSnapshotException exception = assertThrows(NotifyAttachmentSnapshotException.class,
            () -> dispatcher.send(request(List.of(10L, 20L))));

        assertEquals("SNAPSHOT_COPY_FAILED", exception.code());
        assertEquals(0, adapter.calls.get());
    }

    @Test
    void providerFailureAndEventFailureMustRetainSnapshotObjects() {
        RecordingSnapshotService snapshots = new RecordingSnapshotService(false);
        RecordingAdapter adapter = new RecordingAdapter(true);
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, event -> {
                throw new IllegalStateException("listener down");
            }, null, snapshots);

        assertThrows(org.namewta.common.notify.exception.NotifyDeliveryException.class,
            () -> dispatcher.send(request(List.of(10L))));

        assertEquals(1, adapter.calls.get());
        assertEquals(0, snapshots.cleanupCalls.get());
    }

    @Test
    void materializeFailureMustCleanupSnapshotsAndReleaseBeforeProvider() {
        RecordingSnapshotService snapshots = new RecordingSnapshotService(false);
        AtomicInteger calls = new AtomicInteger();
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() {
                return NotifyChannel.of("test");
            }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest request) {
                calls.incrementAndGet();
                throw new NotifyAttachmentSnapshotException("SNAPSHOT_MATERIALIZE_FAILED", "download failed");
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, event -> {
            }, null, snapshots);

        NotifyAttachmentSnapshotException exception = assertThrows(NotifyAttachmentSnapshotException.class,
            () -> dispatcher.send(request(List.of(10L))));

        assertEquals("SNAPSHOT_MATERIALIZE_FAILED", exception.code());
        assertEquals(1, calls.get());
        assertEquals(1, snapshots.cleanupCalls.get());
    }

    @Test
    void explicitUnsentRetryableCleansSnapshotAndPublishesNoStaleAttachmentReference() {
        RecordingSnapshotService snapshots = new RecordingSnapshotService(false);
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override
            public NotifyChannel channel() { return NotifyChannel.of("test"); }

            @Override
            public NotifyAdapterResult send(NotifyAdapterRequest request) {
                assertEquals(List.of(1010L), request.attachments().stream()
                    .map(item -> item.resource().ossId()).toList());
                return new NotifyAdapterResult("provider-a", List.of(
                    NotifyTargetResult.unsentRetryable(request.request().targets().getFirst(),
                        "RATE_LIMIT_30S", 1L)));
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, events::add, null, snapshots);

        assertThrows(org.namewta.common.notify.exception.NotifyDeliveryException.class,
            () -> dispatcher.send(request(List.of(10L))));
        assertEquals(1, snapshots.cleanupCalls.get());
        assertEquals(1, events.size());
        assertNull(events.getFirst().attachmentOwnerIntentId());
        assertTrue(events.getFirst().attachmentSnapshotOssIds().isEmpty());
    }

    @Test
    void preSendGateIsInternalOnlyForFullAndRedactedEvents() {
        for (NotifyAuditPolicy policy : List.of(NotifyAuditPolicy.FULL, NotifyAuditPolicy.REDACT_SENSITIVE)) {
            List<NotifyDeliveryEvent> events = new ArrayList<>();
            NotifyRequest.PreSendGate gate = new NotifyRequest.PreSendGate(() -> { });
            NotifyRequest request = NotifyRequest.builder().requestId("owned-event")
                .channel(NotifyChannel.of("test"))
                .targets(List.of(NotifyTarget.email("to@example.com", NotifyTargetRole.TO)))
                .content(new NotifyRichContent("subject", "content", false))
                .auditPolicy(policy).preSendGate(gate).build();
            dispatcher(new RecordingAdapter(false), new RecordingSnapshotService(false), events).send(request);
            assertEquals(1, events.size());
            assertNull(events.getFirst().request().preSendGate());
            assertFalse(JsonUtils.toJsonString(request).contains("preSendGate"));
            assertFalse(JsonUtils.toJsonString(events.getFirst().request()).contains("preSendGate"));
        }
    }

    private NotifyDispatcher dispatcher(RecordingAdapter adapter, RecordingSnapshotService snapshots,
                                        List<NotifyDeliveryEvent> events) {
        return new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)), NotifyContext::empty,
            events::add, null, snapshots);
    }

    private NotifyRequest request(List<Long> attachments) {
        return NotifyRequest.builder()
            .requestId("request-1")
            .channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.email("to@example.com", NotifyTargetRole.TO)))
            .content(new NotifyRichContent("subject", "content", false))
            .attachmentOssIds(attachments)
            .attachmentOwnerIntentId(attachments.isEmpty() ? null : 7001L)
            .build();
    }

    private static final class RecordingSnapshotService implements NotifyAttachmentSnapshotService {
        private final boolean fail;
        private final AtomicInteger cleanupCalls = new AtomicInteger();
        private List<Long> sources = List.of();
        private long notifyLogId;

        private RecordingSnapshotService(boolean fail) {
            this.fail = fail;
        }

        @Override
        public List<NotifyAttachmentSnapshot> createSnapshots(long logId, List<Long> sourceOssIds,
                                                               NotifyContext context) {
            notifyLogId = logId;
            sources = List.copyOf(sourceOssIds);
            if (fail) {
                throw new NotifyAttachmentSnapshotException("SNAPSHOT_COPY_FAILED", "copy failed");
            }
            return sourceOssIds.stream().map(source -> new NotifyAttachmentSnapshot(source,
                new NotifyAttachmentResource(source + 1000, "file-" + source + ".txt", "text/plain", 4,
                    target -> {
                    }))).toList();
        }

        @Override
        public void cleanupSnapshots(List<NotifyAttachmentSnapshot> values) {
            cleanupCalls.incrementAndGet();
        }
    }

    private static final class RecordingAdapter implements NotifyChannelAdapter {
        private final boolean fail;
        private final AtomicInteger calls = new AtomicInteger();
        private List<Long> snapshotIds = List.of();

        private RecordingAdapter(boolean fail) {
            this.fail = fail;
        }

        @Override
        public NotifyChannel channel() {
            return NotifyChannel.of("test");
        }

        @Override
        public NotifyAdapterResult send(NotifyAdapterRequest request) {
            calls.incrementAndGet();
            snapshotIds = request.attachments().stream().map(item -> item.resource().ossId()).toList();
            NotifyTarget target = request.request().targets().getFirst();
            NotifyTargetResult delivery = fail
                ? NotifyTargetResult.failed(target, "REJECTED", "rejected", 1)
                : NotifyTargetResult.accepted(target, "provider-1", 1);
            return new NotifyAdapterResult("provider-a", List.of(delivery));
        }
    }
}
