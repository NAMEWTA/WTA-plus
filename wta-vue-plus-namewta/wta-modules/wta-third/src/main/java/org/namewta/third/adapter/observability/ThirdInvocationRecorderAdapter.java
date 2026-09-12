package org.namewta.third.adapter.observability;

import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.web.logging.SysLogEventSink;
import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.api.ThirdPartyRequest;
import org.namewta.third.api.ThirdPartyResponse;
import org.namewta.third.port.ThirdInvocationRecorderPort;
import org.namewta.third.port.ThirdOutboundAttempt;
import org.namewta.third.port.ThirdInvocationStore;
import org.namewta.third.port.ThirdStatisticStore;
import org.namewta.third.adapter.log.ThirdLogSanitizerAdapter;
import org.namewta.third.domain.ThirdInvocation;
import org.namewta.third.domain.ThirdStatistic;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

@Component
public class ThirdInvocationRecorderAdapter implements ThirdInvocationRecorderPort {
    private static final Logger log = LoggerFactory.getLogger(ThirdInvocationRecorderAdapter.class);
    private final ThirdInvocationStore invocationDao;
    private final ThirdStatisticStore statisticDao;
    private final ObjectProvider<SysLogEventSink> logSink;
    private final LongAdder logSinkFailureCount = new LongAdder();

    public ThirdInvocationRecorderAdapter(ThirdInvocationStore invocationDao,
                                          ThirdStatisticStore statisticDao,
                                          ObjectProvider<SysLogEventSink> logSink) {
        this.invocationDao = invocationDao;
        this.statisticDao = statisticDao;
        this.logSink = logSink;
    }

    public long logSinkFailureCount() {
        return logSinkFailureCount.sum();
    }

    public void record(ThirdPartyRequest request, ThirdPartyResponse<?> response, long durationMs, int attempts) {
        record(request, response, durationMs, attempts, Set.of());
    }

    @Override
    public void record(ThirdPartyRequest request, ThirdPartyResponse<?> response, long durationMs, int attempts, Set<String> additionalSensitiveFields) {
        try {
            ThirdInvocation invocation = new ThirdInvocation();
            invocation.setInvocationId(IdGeneratorUtil.nextLongId()); invocation.setRequestId(response.requestId());
            invocation.setProviderCode(request.providerCode()); invocation.setEndpointCode(request.endpointCode());
            invocation.setAttemptCount(attempts); invocation.setLogicalStatus(response.isSuccess() ? "SUCCESS" : "FAILURE");
            invocation.setFailureCategory(response.category().name()); invocation.setHttpStatus(response.httpStatus());
            invocation.setDurationMs(durationMs); invocation.setSanitizedRequestJson(ThirdLogSanitizerAdapter.jsonValue(request.body(), additionalSensitiveFields));
            invocation.setSanitizedResponseJson(ThirdLogSanitizerAdapter.jsonValue(response.body(), additionalSensitiveFields)); invocation.setCreateTime(LocalDateTime.now());
            invocationDao.upsert(invocation);
            ThirdStatistic statistic = statistic(request, response, attempts, request.endpointCode());
            statisticDao.upsert(statistic);
            ThirdStatistic providerStatistic = statistic(request, response, attempts, null);
            statisticDao.upsert(providerStatistic);
        } catch (RuntimeException error) {
            log.warn("Third-party invocation recording failed provider={} endpoint={} requestId={}", request.providerCode(), request.endpointCode(), response.requestId());
        }
        try {
            Map<String, Object> event = new LinkedHashMap<>(); event.put("event", "THIRD_HTTP_INVOCATION"); event.put("requestId", response.requestId());
            event.put("providerCode", request.providerCode()); event.put("endpointCode", request.endpointCode()); event.put("failureCategory", response.category().name()); event.put("durationMs", durationMs);
            event.put("attempts", attempts); event.put("parameters", ThirdLogSanitizerAdapter.values(request.query()));
            event.put("requestHeaders", ThirdLogSanitizerAdapter.headers(request.headers())); event.put("body", ThirdLogSanitizerAdapter.json(request.body(), additionalSensitiveFields));
            event.put("response", ThirdLogSanitizerAdapter.value(response.body(), additionalSensitiveFields)); event.put("completed", true);
            writeLog(event);
        } catch (RuntimeException error) {
            log.warn("Third-party invocation log event preparation failed provider={} endpoint={} requestId={}",
                request.providerCode(), request.endpointCode(), response.requestId());
        }
    }

    @Override
    public void recordAttempt(ThirdOutboundAttempt attempt) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("event", attempt.completed() ? "THIRD_HTTP_ATTEMPT_FINISH" : "THIRD_HTTP_ATTEMPT_START");
            event.put("requestId", attempt.requestId());
            event.put("providerCode", attempt.request().providerCode());
            event.put("endpointCode", attempt.request().endpointCode());
            event.put("attempt", attempt.attempt());
            event.put("path", attempt.relativePath());
            event.put("requestHeaders", ThirdLogSanitizerAdapter.headers(attempt.effectiveHeaders()));
            event.put("parameters", ThirdLogSanitizerAdapter.values(attempt.request().query()));
            event.put("body", ThirdLogSanitizerAdapter.value(attempt.requestBody(), attempt.additionalSensitiveFields()));
            event.put("failureCategory", attempt.category() == null ? null : attempt.category().name());
            event.put("httpStatus", attempt.httpStatus());
            event.put("response", ThirdLogSanitizerAdapter.value(attempt.responseBody(), attempt.additionalSensitiveFields()));
            event.put("durationMs", attempt.durationMs());
            event.put("completed", attempt.completed());
            writeLog(event);
        } catch (RuntimeException error) {
            log.warn("Third-party attempt recording failed provider={} endpoint={} requestId={}",
                attempt.request().providerCode(), attempt.request().endpointCode(), attempt.requestId());
        }
    }

    private void writeLog(Map<String, Object> event) {
        try {
            logSink.ifAvailable(sink -> sink.write(event));
        } catch (RuntimeException error) {
            logSinkFailureCount.increment();
            log.warn("Third-party outbound log sink failed requestId={} event={}", event.get("requestId"), event.get("event"));
        }
    }

    private static ThirdStatistic statistic(ThirdPartyRequest request, ThirdPartyResponse<?> response, int attempts, String endpointCode) {
        ThirdStatistic statistic = new ThirdStatistic(); statistic.setStatisticId(IdGeneratorUtil.nextLongId());
        statistic.setProviderCode(request.providerCode()); statistic.setEndpointCode(endpointCode); statistic.setStatDate(LocalDate.now());
        statistic.setAttemptCount((long) attempts); statistic.setSuccessCount(response.isSuccess() ? 1L : 0L);
        statistic.setFailureCount(response.isSuccess() ? 0L : 1L); statistic.setTimeoutCount(response.category() == ThirdPartyFailureCategory.TIMEOUT ? 1L : 0L);
        statistic.setRejectedCount(response.category() == ThirdPartyFailureCategory.RATE_LIMITED || response.category() == ThirdPartyFailureCategory.REJECTED ? 1L : 0L);
        statistic.setQuotaValue(0L); return statistic;
    }
}
