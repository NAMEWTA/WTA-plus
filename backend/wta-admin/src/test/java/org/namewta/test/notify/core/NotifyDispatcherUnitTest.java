package org.namewta.test.notify.core;

import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.notify.core.NotifyDispatcher;
import org.namewta.common.notify.event.NotifyDeliveryEvent;
import org.namewta.common.notify.exception.NotifyDeliveryException;
import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.idempotency.NotifyIdempotencyCoordinator;
import org.namewta.common.notify.idempotency.NotifyIdempotencyProperties;
import org.namewta.common.notify.idempotency.NotifyIdempotencyStore;
import org.namewta.common.notify.model.*;
import org.namewta.common.notify.registry.NotifyChannelRegistry;
import org.namewta.common.notify.spi.NotifyChannelAdapter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * common-notify 调度契约测试。
 */
@Tag("dev")
class NotifyDispatcherUnitTest {

    @Test
    void shouldResolveContextAndPublishAcceptedEvent() {
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        RecordingAdapter adapter = new RecordingAdapter(false);
        NotifyDispatcher dispatcher = new NotifyDispatcher(
            new NotifyChannelRegistry(List.of(adapter)),
            () -> new NotifyContext(12L, 34L, "trace-1"),
            events::add
        );
        NotifyRequest request = NotifyRequest.builder()
            .requestId("request-1")
            .bizType("account")
            .bizId("100")
            .channel(NotifyChannel.of("test"))
            .providerKey("provider-b")
            .targets(List.of(NotifyTarget.phone("13800000000")))
            .content(new NotifyTextContent("subject", "content"))
            .build();

        NotifyResult result = dispatcher.send(request);

        assertEquals(NotifyStatus.ACCEPTED, result.status());
        assertEquals(NotifyChannel.of("test"), result.channel());
        assertEquals("provider-b", result.providerKey());
        assertEquals(1, result.deliveries().size());
        assertEquals(34L, adapter.context.clientPk());
        assertEquals(12L, adapter.context.userId());
        assertEquals("trace-1", events.getFirst().context().traceId());
        assertEquals(result, events.getFirst().result());
    }

    @Test
    void shouldAttemptEveryTargetBeforeThrowingPartialFailure() {
        RecordingAdapter adapter = new RecordingAdapter(true);
        NotifyDispatcher dispatcher = dispatcher(adapter);
        NotifyRequest request = NotifyRequest.builder()
            .channel(NotifyChannel.of("test"))
            .targets(List.of(
                NotifyTarget.phone("13800000000"),
                NotifyTarget.phone("13900000000"),
                NotifyTarget.phone("13700000000")
            ))
            .content(new NotifyTextContent("subject", "content"))
            .build();

        NotifyDeliveryException exception = assertThrows(NotifyDeliveryException.class, () -> dispatcher.send(request));

        assertEquals(3, adapter.attemptedTargets.size());
        assertEquals(NotifyStatus.PARTIAL_FAILURE, exception.result().status());
        assertEquals(2, exception.result().deliveries().stream()
            .filter(item -> item.status() == NotifyDeliveryStatus.ACCEPTED).count());
        assertEquals(1, exception.result().deliveries().stream()
            .filter(item -> item.status() == NotifyDeliveryStatus.FAILED).count());
    }

    @Test
    void shouldRejectLogicalUserAndTemplateWithoutSnapshotBeforeProvider() {
        RecordingAdapter adapter = new RecordingAdapter(false);
        NotifyDispatcher dispatcher = dispatcher(adapter);
        NotifyRequest userRequest = NotifyRequest.builder()
            .channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.user("100")))
            .content(new NotifyTextContent("subject", "content"))
            .build();
        NotifyRequest templateRequest = NotifyRequest.builder()
            .channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.phone("13800000000")))
            .content(new NotifyTemplateContent("subject", "SMS_001", Map.of("code", "123456"), ""))
            .build();

        NotifyValidationException userException = assertThrows(NotifyValidationException.class,
            () -> dispatcher.send(userRequest));
        NotifyValidationException templateException = assertThrows(NotifyValidationException.class,
            () -> dispatcher.send(templateRequest));

        assertEquals("LOGICAL_TARGET_NOT_SUPPORTED", userException.code());
        assertEquals("CONTENT_SNAPSHOT_REQUIRED", templateException.code());
        assertTrue(adapter.attemptedTargets.isEmpty());
    }

    @Test
    void shouldRejectDuplicateChannelAdapters() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> new NotifyChannelRegistry(List.of(new RecordingAdapter(false), new RecordingAdapter(false))));

        assertTrue(exception.getMessage().contains("test"));
    }

    @Test
    void shouldRejectWellFormedButUnregisteredChannel() {
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of()),
            NotifyContext::empty, event -> {
            });
        NotifyRequest request = NotifyRequest.builder()
            .channel(NotifyChannel.of("feishu"))
            .targets(List.of(NotifyTarget.openId("open-id")))
            .content(new NotifyTextContent("subject", "content"))
            .build();

        NotifyValidationException exception = assertThrows(NotifyValidationException.class,
            () -> dispatcher.send(request));

        assertEquals("UNKNOWN_CHANNEL", exception.code());
    }

    @Test
    void shouldNotChangeProviderResultWhenEventPublishingFails() {
        RecordingAdapter adapter = new RecordingAdapter(false);
        NotifyDispatcher dispatcher = new NotifyDispatcher(
            new NotifyChannelRegistry(List.of(adapter)), NotifyContext::empty,
            event -> {
                throw new IllegalStateException("listener unavailable");
            });
        NotifyRequest request = NotifyRequest.builder()
            .channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.phone("13800000000")))
            .content(new NotifyTextContent("subject", "content"))
            .build();

        NotifyResult result = assertDoesNotThrow(() -> dispatcher.send(request));

        assertEquals(NotifyStatus.ACCEPTED, result.status());
    }

    @Test
    void redactedEventCannotExposeRequestOrProviderResultWhileAdapterGetsOriginal() {
        String phone = "13812345678";
        String otp = "otp-canary-7731";
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        NotifyChannelAdapter adapter = new NotifyChannelAdapter() {
            @Override public NotifyChannel channel() { return NotifyChannel.SMS; }
            @Override public NotifyAdapterResult send(NotifyAdapterRequest input) {
                assertEquals(phone, input.request().targets().getFirst().value());
                assertEquals(otp, ((NotifyTemplateContent) input.request().content()).params().get("code"));
                return new NotifyAdapterResult("supplier", List.of(NotifyTargetResult.failed(
                    input.request().targets().getFirst(), "E-" + otp, "rejected " + phone, 1)));
            }
        };
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            () -> new NotifyContext(3L, 4L, phone), events::add);
        NotifyRequest request = NotifyRequest.builder().requestId(phone).bizType("auth")
            .bizId(phone).channel(NotifyChannel.SMS).providerKey("supplier")
            .targets(List.of(new NotifyTarget("type-" + otp, phone, "role-" + phone)))
            .content(new NotifyTemplateContent("subject-" + otp, "template-" + otp,
                Map.of("code", otp), "snapshot-" + otp))
            .metadata(Map.of("secret", otp)).auditPolicy(NotifyAuditPolicy.REDACT_SENSITIVE).build();

        NotifyDeliveryException failure = assertThrows(NotifyDeliveryException.class, () -> dispatcher.send(request));
        assertEquals("E-" + otp, failure.result().deliveries().getFirst().errorCode());
        String emitted = JsonUtils.toJsonString(events.getFirst());
        assertFalse(emitted.contains(phone));
        assertFalse(emitted.contains(otp));
        assertEquals(NotifyAuditPolicy.REDACT_SENSITIVE, events.getFirst().request().auditPolicy());
        assertEquals("[REDACTED]", events.getFirst().result().deliveries().getFirst().target().value());
    }

    @Test
    void redactedDuplicateEventMasksKeyAndOriginalRequestWithoutResending() {
        String phone = "13812345678";
        String otp = "otp-canary-7731";
        List<NotifyDeliveryEvent> events = new ArrayList<>();
        RecordingAdapter adapter = new RecordingAdapter(false);
        NotifyIdempotencyStore store = mock(NotifyIdempotencyStore.class);
        NotifyResult completed = new NotifyResult(phone, NotifyChannel.of("test"), otp,
            NotifyStatus.ACCEPTED, List.of(NotifyTargetResult.accepted(NotifyTarget.phone(phone), otp, 1)));
        when(store.acquire(any(), any(), any(), any()))
            .thenAnswer(call -> new NotifyIdempotencyStore.Acquired(call.getArgument(0), call.getArgument(1),
                call.getArgument(2), "owned", call.getArgument(3)))
            .thenReturn(new NotifyIdempotencyStore.Completed("digest", phone, completed));
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, events::add,
            new NotifyIdempotencyCoordinator(store, new NotifyIdempotencyProperties()));
        NotifyRequest request = NotifyRequest.builder().requestId(phone).channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.phone(phone))).content(new NotifyTextContent(otp, otp))
            .idempotencyKey(phone).auditPolicy(NotifyAuditPolicy.REDACT_SENSITIVE).build();

        assertEquals(NotifyStatus.ACCEPTED, dispatcher.send(request).status());
        assertEquals(NotifyStatus.ACCEPTED, dispatcher.send(request).status());
        assertEquals(1, adapter.attemptedTargets.size());
        assertEquals(2, events.size());
        for (NotifyDeliveryEvent event : events) {
            String emitted = JsonUtils.toJsonString(event);
            assertFalse(emitted.contains(phone));
            assertFalse(emitted.contains(otp));
        }
    }

    @Test
    void failedEventPublisherLogsNoSensitiveRequestIdForNormalOrDuplicate() {
        String phone = "13812345678";
        RecordingAdapter adapter = new RecordingAdapter(false);
        NotifyIdempotencyStore store = mock(NotifyIdempotencyStore.class);
        NotifyResult completed = new NotifyResult(phone, NotifyChannel.of("test"), "provider",
            NotifyStatus.ACCEPTED, List.of());
        when(store.acquire(any(), any(), any(), any()))
            .thenAnswer(call -> new NotifyIdempotencyStore.Acquired(call.getArgument(0), call.getArgument(1),
                call.getArgument(2), "owned", call.getArgument(3)))
            .thenReturn(new NotifyIdempotencyStore.Completed("digest", phone, completed));
        NotifyDispatcher dispatcher = new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)),
            NotifyContext::empty, event -> { throw new IllegalStateException("listener unavailable"); },
            new NotifyIdempotencyCoordinator(store, new NotifyIdempotencyProperties()));
        NotifyRequest request = NotifyRequest.builder().requestId(phone).channel(NotifyChannel.of("test"))
            .targets(List.of(NotifyTarget.phone(phone))).content(new NotifyTextContent("subject", "content"))
            .idempotencyKey(phone).auditPolicy(NotifyAuditPolicy.REDACT_SENSITIVE).build();
        Logger logger = (Logger) LoggerFactory.getLogger(NotifyDispatcher.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertEquals(NotifyStatus.ACCEPTED, dispatcher.send(request).status());
            assertEquals(NotifyStatus.ACCEPTED, dispatcher.send(request).status());
            assertEquals(2, appender.list.size());
            assertTrue(appender.list.stream().noneMatch(event -> event.getFormattedMessage().contains(phone)));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private NotifyDispatcher dispatcher(NotifyChannelAdapter adapter) {
        return new NotifyDispatcher(new NotifyChannelRegistry(List.of(adapter)), NotifyContext::empty, event -> {
        });
    }

    private static final class RecordingAdapter implements NotifyChannelAdapter {

        private final boolean failSecond;
        private final List<NotifyTarget> attemptedTargets = new ArrayList<>();
        private NotifyContext context;

        private RecordingAdapter(boolean failSecond) {
            this.failSecond = failSecond;
        }

        @Override
        public NotifyChannel channel() {
            return NotifyChannel.of("test");
        }

        @Override
        public NotifyAdapterResult send(NotifyAdapterRequest request) {
            context = request.context();
            List<NotifyTargetResult> results = new ArrayList<>();
            for (int index = 0; index < request.request().targets().size(); index++) {
                NotifyTarget target = request.request().targets().get(index);
                attemptedTargets.add(target);
                if (failSecond && index == 1) {
                    results.add(NotifyTargetResult.failed(target, "PROVIDER_REJECTED", "provider rejected", 4L));
                } else {
                    results.add(NotifyTargetResult.accepted(target, "message-" + index, 3L));
                }
            }
            String provider = request.request().providerKey() == null ? "provider-a" : request.request().providerKey();
            return new NotifyAdapterResult(provider, results);
        }
    }
}
