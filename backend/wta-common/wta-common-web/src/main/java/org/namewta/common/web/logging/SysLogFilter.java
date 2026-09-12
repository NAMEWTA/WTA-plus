package org.namewta.common.web.logging;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.namewta.common.web.filter.RepeatedlyRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 在 Servlet Filter 边界记录一对可关联的请求与响应 JSON 事件。
 */
public class SysLogFilter implements Filter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";

    private static final String STATE_ATTRIBUTE = SysLogFilter.class.getName() + ".exchange";
    private static final String REDACTED_HEADER_VALUE = "[REDACTED]";
    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
        "authorization",
        "proxy-authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
        "api-key",
        "x-app-key",
        "x-signature",
        "x-auth-token",
        "x-csrf-token",
        "encrypt-key"
    );
    private static final Logger FAILURE_LOG = LoggerFactory.getLogger(SysLogFilter.class);

    private final int maxBodyBytes;
    private final SysLogEventSink eventSink;

    public SysLogFilter(int maxBodyBytes, SysLogEventSink eventSink) {
        if (maxBodyBytes <= 0) {
            throw new IllegalArgumentException("正文日志字节上限必须大于 0");
        }
        this.maxBodyBytes = maxBodyBytes;
        this.eventSink = eventSink;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
            || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        ExchangeState state = (ExchangeState) httpRequest.getAttribute(STATE_ATTRIBUTE);
        HttpServletRequest effectiveRequest = httpRequest;
        boolean initialDispatch = state == null;
        if (initialDispatch) {
            effectiveRequest = prepareRequest(httpRequest, httpResponse);
            state = new ExchangeState(effectiveRequest, maxBodyBytes);
            effectiveRequest.setAttribute(STATE_ATTRIBUTE, state);
            httpResponse.setHeader(REQUEST_ID_HEADER, state.requestId());
        }

        SysLogResponseWrapper effectiveResponse = wrapResponse(httpResponse, state.responseCapture());
        state.updateResponse(effectiveResponse);
        String previousRequestId = MDC.get(MDC_REQUEST_ID);
        MDC.put(MDC_REQUEST_ID, state.requestId());
        try {
            if (initialDispatch) {
                emit(state.requestEvent(effectiveRequest));
            }
            chain.doFilter(effectiveRequest, effectiveResponse);
            if (effectiveRequest.isAsyncStarted()) {
                state.registerAsync(effectiveRequest.getAsyncContext(), effectiveRequest, effectiveResponse);
            } else {
                state.finish(true, effectiveResponse);
            }
        } catch (IOException | ServletException | RuntimeException | Error exception) {
            state.finish(false, effectiveResponse);
            throw exception;
        } finally {
            restoreMdc(previousRequestId);
        }
    }

    private HttpServletRequest prepareRequest(HttpServletRequest request, HttpServletResponse response) {
        if (request instanceof RepeatedlyRequestWrapper
            || !SysLogMediaTypePolicy.isRequestBodyLoggable(request.getContentType())) {
            return request;
        }
        try {
            return new RepeatedlyRequestWrapper(request, response);
        } catch (IOException | RuntimeException exception) {
            reportFailure("请求正文采集", null, exception);
            return request;
        }
    }

    private SysLogResponseWrapper wrapResponse(HttpServletResponse response, SysLogResponseCapture capture) {
        if (response instanceof SysLogResponseWrapper wrapper && wrapper.uses(capture)) {
            return wrapper;
        }
        return new SysLogResponseWrapper(response, capture);
    }

    private void emit(Map<String, Object> event) {
        try {
            eventSink.write(event);
        } catch (RuntimeException | LinkageError exception) {
            String eventName = String.valueOf(event.get("event"));
            String stage = switch (eventName) {
                case "HTTP_REQUEST" -> "请求事件输出";
                case "HTTP_RESPONSE" -> "响应事件输出";
                default -> eventName;
            };
            reportFailure(stage, String.valueOf(event.get("requestId")), exception);
        }
    }

    private void reportFailure(String stage, String requestId, Throwable failure) {
        try {
            FAILURE_LOG.error("系统 HTTP 日志采集失败：阶段={}，请求标识={}", stage, requestId, failure);
        } catch (RuntimeException | LinkageError terminalFailure) {
            System.err.printf("系统 HTTP 日志采集失败：阶段=%s，请求标识=%s，原因=%s%n",
                stage, requestId, failure.getMessage());
        }
    }

    private void restoreMdc(String requestId) {
        if (requestId == null) {
            MDC.remove(MDC_REQUEST_ID);
        } else {
            MDC.put(MDC_REQUEST_ID, requestId);
        }
    }

    private Map<String, List<String>> requestHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        try {
            Enumeration<String> names = request.getHeaderNames();
            if (names == null) {
                return headers;
            }
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headers.put(name, isSensitiveHeader(name)
                    ? List.of(REDACTED_HEADER_VALUE)
                    : enumerationValues(request.getHeaders(name)));
            }
        } catch (RuntimeException exception) {
            reportFailure("请求头采集", null, exception);
        }
        return headers;
    }

    private Map<String, List<String>> requestParameters(HttpServletRequest request) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        try {
            request.getParameterMap().forEach((name, values) ->
                parameters.put(name, SysLogBodySanitizer.isSensitiveName(name)
                    ? List.of(REDACTED_HEADER_VALUE)
                    : values == null ? List.of() : Arrays.asList(values.clone())));
        } catch (RuntimeException exception) {
            reportFailure("请求参数采集", null, exception);
        }
        return parameters;
    }

    private Map<String, List<String>> responseHeaders(HttpServletResponse response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        try {
            for (String name : response.getHeaderNames()) {
                headers.put(name, isSensitiveHeader(name)
                    ? List.of(REDACTED_HEADER_VALUE)
                    : new ArrayList<>(response.getHeaders(name)));
            }
        } catch (RuntimeException exception) {
            reportFailure("响应头采集", null, exception);
        }
        return headers;
    }

    private boolean isSensitiveHeader(String name) {
        return name != null && SENSITIVE_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    private List<String> enumerationValues(Enumeration<String> values) {
        List<String> result = new ArrayList<>();
        if (values != null) {
            while (values.hasMoreElements()) {
                result.add(values.nextElement());
            }
        }
        return result;
    }

    private SysLogBody requestBody(HttpServletRequest request, int limit) {
        long contentLength = request.getContentLengthLong();
        if (contentLength == 0) {
            return SysLogBody.omitted(0, "NO_BODY");
        }
        SysLogMediaTypePolicy.BodyDecision decision = SysLogMediaTypePolicy.requestDecision(request.getContentType());
        if (!decision.loggable()) {
            return SysLogBody.omitted(0, decision.omissionReason());
        }
        if (request instanceof RepeatedlyRequestWrapper wrapper) {
            if (wrapper.getBodyLength() == 0) {
                return SysLogBody.omitted(0, "NO_BODY");
            }
            return SysLogBody.logged(wrapper.getBodyPrefix(limit), wrapper.getBodyLength(), limit);
        }
        return SysLogBody.omitted(0, "BODY_CAPTURE_UNAVAILABLE");
    }

    private long responseContentLength(HttpServletResponse response) {
        String value = response.getHeader("Content-Length");
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void putBody(Map<String, Object> event, SysLogBody body, String contentType) {
        body = SysLogBodySanitizer.sanitize(body, contentType);
        event.put("bodyLogged", body.logged());
        event.put("bodyLength", body.length());
        event.put("truncated", body.truncated());
        if (body.logged()) {
            event.put("body", body.body());
        } else {
            event.put("bodyOmissionReason", body.omissionReason());
        }
    }

    private final class ExchangeState {

        private final String requestId = UUID.randomUUID().toString();
        private final String method;
        private final String path;
        private final long startNanos = System.nanoTime();
        private final int bodyLimit;
        private final SysLogResponseCapture responseCapture;
        private final AtomicBoolean finished = new AtomicBoolean();
        private final CompletionListener completionListener = new CompletionListener(this);
        private volatile HttpServletResponse currentResponse;
        private AsyncContext registeredAsyncContext;

        private ExchangeState(HttpServletRequest request, int bodyLimit) {
            this.method = request.getMethod();
            this.path = request.getRequestURI();
            this.bodyLimit = bodyLimit;
            this.responseCapture = new SysLogResponseCapture(bodyLimit);
        }

        private String requestId() {
            return requestId;
        }

        private SysLogResponseCapture responseCapture() {
            return responseCapture;
        }

        private void updateResponse(HttpServletResponse response) {
            currentResponse = response;
        }

        private Map<String, Object> requestEvent(HttpServletRequest request) {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("timestamp", Instant.now().toString());
            event.put("event", "HTTP_REQUEST");
            event.put("requestId", requestId);
            event.put("method", method);
            event.put("path", path);
            event.put("queryString", request.getQueryString());
            event.put("parameters", requestParameters(request));
            event.put("requestHeaders", requestHeaders(request));
            event.put("contentType", request.getContentType());
            event.put("contentLength", request.getContentLengthLong());
            String upstreamRequestId = request.getHeader(REQUEST_ID_HEADER);
            if (upstreamRequestId != null) {
                event.put("upstreamRequestId", upstreamRequestId);
            }
            putBody(event, requestBody(request, bodyLimit), request.getContentType());
            return event;
        }

        private synchronized void registerAsync(AsyncContext asyncContext,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
            if (finished.get() || registeredAsyncContext == asyncContext) {
                return;
            }
            try {
                asyncContext.addListener(completionListener, request, response);
                registeredAsyncContext = asyncContext;
            } catch (IllegalStateException exception) {
                reportFailure("异步监听器注册", requestId, exception);
                finish(false, response);
            }
        }

        private synchronized void asyncRestarted(AsyncContext asyncContext,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
            registeredAsyncContext = null;
            registerAsync(asyncContext, request, response);
        }

        private void finish(boolean completed, HttpServletResponse suppliedResponse) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            HttpServletResponse response = suppliedResponse == null ? currentResponse : suppliedResponse;
            if (response == null) {
                return;
            }
            String previousRequestId = MDC.get(MDC_REQUEST_ID);
            MDC.put(MDC_REQUEST_ID, requestId);
            try {
                if (response instanceof SysLogResponseWrapper wrapper) {
                    wrapper.flushForLogging();
                }
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("timestamp", Instant.now().toString());
                event.put("event", "HTTP_RESPONSE");
                event.put("requestId", requestId);
                event.put("method", method);
                event.put("path", path);
                event.put("status", response.getStatus());
                event.put("responseHeaders", responseHeaders(response));
                event.put("contentType", response.getContentType());
                event.put("contentLength", responseContentLength(response));
                event.put("durationMs", Math.max(0, (System.nanoTime() - startNanos) / 1_000_000));
                event.put("completed", completed);
                putBody(event, responseCapture.snapshot(response), response.getContentType());
                emit(event);
            } finally {
                restoreMdc(previousRequestId);
            }
        }
    }

    private final class CompletionListener implements AsyncListener {

        private final ExchangeState state;

        private CompletionListener(ExchangeState state) {
            this.state = state;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            state.finish(true, suppliedResponse(event));
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            state.finish(false, suppliedResponse(event));
        }

        @Override
        public void onError(AsyncEvent event) {
            state.finish(false, suppliedResponse(event));
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            ServletRequest request = event.getSuppliedRequest();
            ServletResponse response = event.getSuppliedResponse();
            if (request instanceof HttpServletRequest httpRequest
                && response instanceof HttpServletResponse httpResponse) {
                state.asyncRestarted(event.getAsyncContext(), httpRequest, httpResponse);
            }
        }

        private HttpServletResponse suppliedResponse(AsyncEvent event) {
            return event.getSuppliedResponse() instanceof HttpServletResponse response ? response : null;
        }
    }
}
