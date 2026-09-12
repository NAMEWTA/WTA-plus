package org.namewta.test.third.logging;

import ch.qos.logback.classic.LoggerContext;
import org.namewta.common.web.logging.SysLogEventSink;
import org.namewta.common.web.logging.SysLogEventWriter;
import org.namewta.third.adapter.observability.ThirdInvocationRecorderAdapter;
import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.api.ThirdPartyRequest;
import org.namewta.third.port.ThirdOutboundAttempt;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Set;

public final class ThirdOutboundSysLogProcessProbe {

    private ThirdOutboundSysLogProcessProbe() { }

    public static void main(String[] args) {
        JsonMapper mapper = JsonMapper.builder().build();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("sysLogEventWriter", new SysLogEventWriter(mapper));
        ThirdInvocationRecorderAdapter recorder = new ThirdInvocationRecorderAdapter(
            invocation -> 1, statistic -> 1, beanFactory.getBeanProvider(SysLogEventSink.class));
        String largeValue = "x".repeat(20_000);
        ThirdPartyRequest request = new ThirdPartyRequest("qichacha", "company", Map.of(),
            Map.of("token", "raw-query-secret"),
            Map.of("Authorization", "Bearer raw-secret", "X-QCC-Key", "raw-api-key"),
            mapper.readTree("{\"apiKey\":\"request-secret\",\"description\":\"" + largeValue + "\"}"));

        recorder.recordAttempt(new ThirdOutboundAttempt(request, "third-log-canary", 1, "/company/1",
            request.headers(), request.body(), null, null, null, 0, false, Set.of()));
        recorder.recordAttempt(new ThirdOutboundAttempt(request, "third-log-canary", 1, "/company/1",
            request.headers(), request.body(), 200, ThirdPartyFailureCategory.NONE,
            mapper.readTree("{\"token\":\"response-secret\",\"result\":\"ok\"}"), 12, true, Set.of()));

        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) context.stop();
    }
}
