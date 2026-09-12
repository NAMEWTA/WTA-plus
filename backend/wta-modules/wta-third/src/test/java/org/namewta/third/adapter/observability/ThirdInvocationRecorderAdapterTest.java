package org.namewta.third.adapter.observability;

import org.namewta.common.web.logging.SysLogEventSink;
import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.api.ThirdPartyRequest;
import org.namewta.third.api.ThirdPartyResponse;
import org.namewta.third.domain.ThirdInvocation;
import org.namewta.third.domain.ThirdStatistic;
import org.namewta.third.port.ThirdInvocationStore;
import org.namewta.third.port.ThirdOutboundAttempt;
import org.namewta.third.port.ThirdStatisticStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
class ThirdInvocationRecorderAdapterTest {
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void persistsOnlySanitizedDetailsAndWritesSanitizedSysLogEvent() {
        List<ThirdInvocation> invocations = new ArrayList<>();
        List<ThirdStatistic> statistics = new ArrayList<>();
        List<Map<String, Object>> events = new ArrayList<>();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("thirdTestSink", (SysLogEventSink) events::add);
        ObjectProvider<SysLogEventSink> sink = beanFactory.getBeanProvider(SysLogEventSink.class);
        ThirdInvocationStore invocationStore = invocation -> {
            invocations.add(invocation);
            return 1;
        };
        ThirdStatisticStore statisticStore = statistic -> {
            statistics.add(statistic);
            return 1;
        };
        ThirdInvocationRecorderAdapter recorder = new ThirdInvocationRecorderAdapter(invocationStore, statisticStore, sink);
        ThirdPartyRequest request = new ThirdPartyRequest("qichacha", "company", Map.of(), Map.of("token", "secret-token"),
            Map.of("Authorization", "Bearer secret-token"), jsonMapper.readTree("{\"apiKey\":\"plain-secret\",\"name\":\"Acme\"}"));
        ThirdPartyResponse<Object> response = new ThirdPartyResponse<>("request-1", "qichacha", "company", 200,
            ThirdPartyFailureCategory.NONE, null, jsonMapper.readTree("{\"token\":\"response-secret\",\"result\":\"ok\"}"));

        recorder.record(request, response, 12, 1);

        assertEquals(1, invocations.size());
        assertEquals(2, statistics.size(), "endpoint and provider aggregates are both recorded");
        assertEquals(1, events.size());
        assertNotNull(invocations.getFirst().getSanitizedRequestJson());
        assertFalse(invocations.getFirst().getSanitizedRequestJson().contains("plain-secret"));
        assertFalse(invocations.getFirst().getSanitizedRequestJson().contains("secret-token"));
        assertTrue(invocations.getFirst().getSanitizedRequestJson().contains("***"));
        Map<String, Object> event = events.getFirst();
        assertEquals("***", ((Map<?, ?>) event.get("requestHeaders")).get("Authorization"));
        assertEquals("***", ((Map<?, ?>) event.get("parameters")).get("token"));
        assertFalse(String.valueOf(event.get("body")).contains("plain-secret"));
        assertFalse(String.valueOf(event.get("response")).contains("response-secret"));
    }

    @Test
    void recordsEachPhysicalAttemptWithSanitizedFields() {
        List<Map<String, Object>> events = new ArrayList<>();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("thirdTestSink", (SysLogEventSink) events::add);
        ThirdInvocationRecorderAdapter recorder = new ThirdInvocationRecorderAdapter(
            invocation -> 1, statistic -> 1, beanFactory.getBeanProvider(SysLogEventSink.class));
        ThirdPartyRequest request = new ThirdPartyRequest("qichacha", "company", Map.of(), Map.of(), Map.of(),
            jsonMapper.readTree("{\"apiKey\":\"plain-secret\"}"));

        recorder.recordAttempt(new ThirdOutboundAttempt(request, "request-2", 1, "/companies/1",
            Map.of("Authorization", "Bearer secret-token", "X-QCC-Key", "plain-key"), request.body(), null, null, null, 0, false, Set.of()));
        recorder.recordAttempt(new ThirdOutboundAttempt(request, "request-2", 1, "/companies/1",
            Map.of("Authorization", "Bearer secret-token", "X-QCC-Key", "plain-key"), request.body(), 200, ThirdPartyFailureCategory.NONE,
            jsonMapper.readTree("{\"token\":\"response-secret\"}"), 4, true, Set.of()));

        assertEquals(2, events.size());
        assertEquals("THIRD_HTTP_ATTEMPT_START", events.getFirst().get("event"));
        assertEquals("THIRD_HTTP_ATTEMPT_FINISH", events.getLast().get("event"));
        assertEquals(ThirdPartyFailureCategory.NONE.name(), events.getLast().get("failureCategory"));
        assertEquals(200, events.getLast().get("httpStatus"));
        assertEquals("***", ((Map<?, ?>) events.getFirst().get("requestHeaders")).get("Authorization"));
        assertEquals("***", ((Map<?, ?>) events.getFirst().get("requestHeaders")).get("X-QCC-Key"));
        assertFalse(String.valueOf(events.getLast().get("body")).contains("plain-secret"));
        assertFalse(String.valueOf(events.getLast().get("response")).contains("response-secret"));
    }

    @Test
    void exposesSinkFailureThroughHealthIndicator() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("thirdFailingSink", (SysLogEventSink) event -> {
            throw new IllegalStateException("sink unavailable");
        });
        ThirdInvocationRecorderAdapter recorder = new ThirdInvocationRecorderAdapter(
            invocation -> 1, statistic -> 1, beanFactory.getBeanProvider(SysLogEventSink.class));
        ThirdPartyRequest request = ThirdPartyRequest.of("qichacha", "company");

        recorder.recordAttempt(new ThirdOutboundAttempt(request, "request-3", 1, "/companies/1",
            Map.of(), null, null, null, null, 0, false, Set.of()));

        assertEquals(1, recorder.logSinkFailureCount());
        assertEquals("DOWN", new ThirdInvocationLogHealthIndicator(recorder).health().getStatus().getCode());
    }
}
