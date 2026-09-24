package org.namewta.common.web.logging;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.ServletException;
import org.namewta.common.web.filter.RepeatedlyRequestWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class SysLogFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void recordsCompleteRequestAndResponseWithServerRequestId() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(1024 * 1024, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest request = jsonRequest("{\"password\":\"plain\",\"token\":\"raw\"}");
        request.setQueryString("page=1&page=2");
        request.addParameter("page", "1", "2");
        request.addParameter("password", "plain-parameter");
        request.addHeader("Authorization", "Bearer raw-token");
        request.addHeader("Cookie", "session=raw-cookie");
        request.addHeader("X-Api-Key", "raw-api-key");
        request.addHeader("X-App-Key", "raw-app-key");
        request.addHeader("X-Signature", "raw-signature");
        request.addHeader(SysLogFilter.REQUEST_ID_HEADER, "client-request-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .contains("plain", "raw");
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) servletResponse;
            httpResponse.setStatus(201);
            httpResponse.setContentType("application/json");
            httpResponse.addHeader("Set-Cookie", "refresh=raw-cookie");
            httpResponse.getWriter().write("{\"code\":200,\"data\":{\"accessToken\":\"response-raw\"}}");
        });

        assertThat(events).hasSize(2);
        Map<String, Object> requestEvent = events.getFirst();
        Map<String, Object> responseEvent = events.getLast();
        String requestId = (String) requestEvent.get("requestId");
        assertThatCodeIsUuid(requestId);
        assertThat(requestId).isNotEqualTo("client-request-id");
        assertThat(response.getHeader(SysLogFilter.REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertThat(responseEvent.get("requestId")).isEqualTo(requestId);
        assertThat(requestEvent)
            .containsEntry("event", "HTTP_REQUEST")
            .containsEntry("upstreamRequestId", "client-request-id")
            .containsEntry("bodyLogged", true)
            .containsEntry("body", "{\"password\":\"[REDACTED]\",\"token\":\"[REDACTED]\"}");
        assertThat(((Map<?, ?>) requestEvent.get("requestHeaders")).get("Authorization"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(((Map<?, ?>) requestEvent.get("requestHeaders")).get("Cookie"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(((Map<?, ?>) requestEvent.get("requestHeaders")).get("X-Api-Key"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(((Map<?, ?>) requestEvent.get("requestHeaders")).get("X-App-Key"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(((Map<?, ?>) requestEvent.get("requestHeaders")).get("X-Signature"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(((Map<?, ?>) requestEvent.get("parameters")).get("page"))
            .isEqualTo(List.of("1", "2"));
        assertThat(((Map<?, ?>) requestEvent.get("parameters")).get("password"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(responseEvent)
            .containsEntry("event", "HTTP_RESPONSE")
            .containsEntry("status", 201)
            .containsEntry("completed", true)
            .containsEntry("bodyLogged", true)
            .containsEntry("body", "{\"code\":200,\"data\":{\"accessToken\":\"[REDACTED]\"}}");
        assertThat(((Map<?, ?>) responseEvent.get("responseHeaders")).get("Set-Cookie"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(response.getContentAsString())
            .isEqualTo("{\"code\":200,\"data\":{\"accessToken\":\"response-raw\"}}");
        assertThat(MDC.get(SysLogFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void onlineOperationPathsAndListTokenIdAreSafeOnlyInHttpEvents() throws Exception {
        String token = "credential-canary-online-filter";
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest listRequest = new MockHttpServletRequest("GET", "/monitor/online/list");
        MockHttpServletResponse listResponse = new MockHttpServletResponse();
        filter.doFilter(listRequest, listResponse, (request, response) -> {
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) response;
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"rows\":[{\"tokenId\":\"" + token + "\",\"userName\":\"visible\"}]}");
        });
        assertThat(events).hasSize(2);
        assertThat(events.getFirst()).containsEntry("path", "/monitor/online/list");
        assertThat(events.getLast()).containsEntry("path", "/monitor/online/list");
        assertThat(events.getLast().toString()).doesNotContain(token).contains("visible");
        assertThat(listResponse.getContentAsString()).contains(token);

        MockHttpServletRequest operation = new MockHttpServletRequest("POST", "/monitor/online/myself/" + token);
        filter.doFilter(operation, new MockHttpServletResponse(), (request, response) -> {
            assertThat(((jakarta.servlet.http.HttpServletRequest) request).getRequestURI()).contains(token);
        });
        assertThat(events).hasSize(4);
        assertThat(events.get(2)).containsEntry("path", "/monitor/online/myself/[REDACTED]");
        assertThat(events.get(3)).containsEntry("path", "/monitor/online/myself/[REDACTED]");
        assertThat(events.toString()).doesNotContain(token);
    }

    @Test
    void ossSignedResponseAndUploadPathAreRedactedWithoutChangingBusinessResponse() throws Exception {
        String accessKey = "AK-CANARY-FILTER-7741";
        String signature = "SIGNATURE-CANARY-FILTER-7741";
        String uploadToken = "UPLOAD-TOKEN-CANARY-FILTER-7741";
        String signedUrl = "https://storage.example.test/private/object?X-Amz-Credential="
            + accessKey + "%2F20260923%2Ftest%2Fs3%2Faws4_request&X-Amz-Signature=" + signature;
        String publicUrl = "https://cdn.example.test/public/object?page=2";
        String body = "{\"code\":200,\"data\":{\"uploadToken\":\"" + uploadToken
            + "\",\"presignedRequest\":{\"url\":\"" + signedUrl
            + "\"},\"parts\":[{\"url\":\"" + signedUrl
            + "\"}],\"publicUrl\":\"" + publicUrl + "\"}}";
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(8192, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest init = new MockHttpServletRequest("POST", "/resource/oss/uploads");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(init, response, (servletRequest, servletResponse) -> {
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) servletResponse;
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(body);
        });

        assertThat(events).hasSize(2);
        assertThat(events.getLast()).containsEntry("bodyLogged", true);
        assertThat(events.getLast().get("body").toString())
            .doesNotContain(accessKey, signature, uploadToken)
            .contains(publicUrl, "200");
        assertThat(response.getContentAsString()).isEqualTo(body);

        MockHttpServletRequest resume = new MockHttpServletRequest("GET",
            "/resource/oss/uploads/" + uploadToken + "/parts");
        resume.addParameter("X-Amz-Credential", accessKey);
        filter.doFilter(resume, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
            assertThat(((jakarta.servlet.http.HttpServletRequest) servletRequest).getRequestURI())
                .contains(uploadToken));
        assertThat(events).hasSize(4);
        assertThat(events.get(2)).containsEntry("path", "/resource/oss/uploads/[REDACTED]/parts");
        assertThat(events.get(3)).containsEntry("path", "/resource/oss/uploads/[REDACTED]/parts");
        assertThat(((Map<?, ?>) events.get(2).get("parameters")).get("X-Amz-Credential"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(events.toString()).doesNotContain(accessKey, signature, uploadToken);
    }

    @Test
    void concealsTruncatedJsonInsteadOfFallingBackToSensitivePlaintext() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(20, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest request = jsonRequest("{\"password\":\"plain\",\"visible\":true}");

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
            assertThat(new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .contains("plain", "visible"));

        assertThat(events.getFirst())
            .containsEntry("bodyLogged", true)
            .containsEntry("body", "[REDACTED]")
            .containsEntry("truncated", true);
    }

    @Test
    void recursivelyRedactsOpenApiCredentialMaterial() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest request = jsonRequest("""
            {"appSecret":"secret-value","nested":{"signature":"signature-value",\
            "machine_token":"machine-token-value","safe":"visible"}}""");

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
        });

        assertThat(events.getFirst().get("body")).isEqualTo(
            "{\"appSecret\":\"[REDACTED]\",\"nested\":{\"signature\":\"[REDACTED]\","
                + "\"machine_token\":\"[REDACTED]\",\"safe\":\"visible\"}}");
        assertThat(events.getFirst().toString())
            .doesNotContain("secret-value", "signature-value", "machine-token-value");
    }

    @Test
    void hidesUnstructuredTextWithoutChangingUtf8BusinessBody() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(5, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/unicode");
        request.setContentType("text/plain");
        request.setContent("ééé".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("ééé");
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) servletResponse;
            httpResponse.setContentType("text/plain");
            httpResponse.getOutputStream().write("ééé".getBytes(StandardCharsets.UTF_8));
        });

        assertThat(events).hasSize(2);
        assertThat(events.getFirst())
            .containsEntry("body", "[REDACTED]")
            .containsEntry("bodyLength", 6L)
            .containsEntry("truncated", true);
        assertThat(events.getLast())
            .containsEntry("body", "[REDACTED]")
            .containsEntry("bodyLength", 6L)
            .containsEntry("truncated", true);
        assertThat(response.getContentAsString()).isEqualTo("ééé");
    }

    @Test
    void omitsBinaryResponseBodyButKeepsMetadata() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/download");
        MockHttpServletResponse response = new MockHttpServletResponse();
        byte[] bytes = {0, 1, 2, 3};

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) servletResponse;
            httpResponse.setContentType("application/octet-stream");
            httpResponse.setContentLength(bytes.length);
            httpResponse.getOutputStream().write(bytes);
        });

        assertThat(events.getLast())
            .containsEntry("bodyLogged", false)
            .containsEntry("bodyLength", 4L)
            .containsEntry("bodyOmissionReason", "NON_TEXT_CONTENT_TYPE")
            .containsEntry("contentLength", 4L);
        assertThat(response.getContentAsByteArray()).containsExactly(bytes);
    }

    @Test
    void preservesUnhandledExceptionAndMarksResponseIncomplete() {
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalStateException cause = new IllegalStateException("root");
        ServletException failure = new ServletException("failed", cause);

        assertThatThrownBy(() -> filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw failure;
        })).isSameAs(failure).hasCause(cause);

        assertThat(events).hasSize(2);
        assertThat(events.getLast()).containsEntry("completed", false);
        assertThat(MDC.get(SysLogFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void completesAsyncResponseExactlyOnce() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/async");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<AsyncContext> async = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
            async.set(servletRequest.startAsync(servletRequest, servletResponse)));

        assertThat(events).hasSize(1);
        var asyncResponse = (jakarta.servlet.http.HttpServletResponse) async.get().getResponse();
        asyncResponse.setContentType("application/json");
        asyncResponse.getWriter().write("{\"done\":true}");
        async.get().complete();

        assertThat(events).hasSize(2);
        assertThat(events.getLast())
            .containsEntry("completed", true)
            .containsEntry("body", "{\"done\":true}");
        assertThat(MDC.get(SysLogFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void recordsAsyncTimeoutOnceAndRestoresCallbackMdc() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        List<String> eventRequestIds = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, event -> {
            events.add(event);
            eventRequestIds.add(MDC.get(SysLogFilter.MDC_REQUEST_ID));
        });
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/async-timeout");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<AsyncContext> async = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
            async.set(servletRequest.startAsync(servletRequest, servletResponse)));

        MockAsyncContext context = (MockAsyncContext) async.get();
        var listener = context.getListeners().getFirst();
        listener.onTimeout(new AsyncEvent(context, context.getRequest(), context.getResponse()));
        listener.onComplete(new AsyncEvent(context, context.getRequest(), context.getResponse()));

        assertThat(events).hasSize(2);
        assertThat(events.getLast()).containsEntry("completed", false);
        assertThat(eventRequestIds).containsOnly((String) events.getFirst().get("requestId"));
        assertThat(MDC.get(SysLogFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void excludesMultipartRequestAndSseResponseBodies() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, events::add);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/stream");
        request.setContentType("multipart/form-data; boundary=test");
        request.setContent("raw-file-content".getBytes(StandardCharsets.UTF_8));
        request.addParameter("description", "original-name");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) servletResponse;
            httpResponse.setContentType("text/event-stream");
            httpResponse.getOutputStream().write("data: raw-event\n\n".getBytes(StandardCharsets.UTF_8));
        });

        assertThat(events.getFirst())
            .containsEntry("bodyLogged", false)
            .containsEntry("bodyOmissionReason", "MULTIPART");
        assertThat(((Map<?, ?>) events.getFirst().get("parameters")).get("description"))
            .isEqualTo(List.of("original-name"));
        assertThat(events.getLast())
            .containsEntry("bodyLogged", false)
            .containsEntry("bodyLength", 17L)
            .containsEntry("bodyOmissionReason", "STREAMING");
        assertThat(response.getContentAsString()).isEqualTo("data: raw-event\n\n");
    }

    @Test
    void preservesPlainJsonThroughNestedWrappersAndRedactsAuditCopies() throws Exception {
        String requestBody = "{\"password\":\"request-secret-canary\",\"displayName\":\"测试\"}";
        String responseBody = "{\"token\":\"response-secret-canary\",\"code\":200}";
        MockHttpServletRequest request = jsonRequest(requestBody);
        request.addHeader("Authorization", "Bearer header-secret-canary");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var repeatableRequest = new RepeatedlyRequestWrapper(request, response, 2 * 1024 * 1024);
        var nestedRequest = new jakarta.servlet.http.HttpServletRequestWrapper(repeatableRequest);
        var nestedResponse = new jakarta.servlet.http.HttpServletResponseWrapper(response);
        List<Map<String, Object>> events = new ArrayList<>();

        new SysLogFilter(1024, 2 * 1024 * 1024, events::add).doFilter(nestedRequest, nestedResponse,
            (servletRequest, servletResponse) -> {
                assertThat(new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo(requestBody);
                assertThat(new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo(requestBody);
                var httpResponse = (jakarta.servlet.http.HttpServletResponse) servletResponse;
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(responseBody);
        });

        assertThat(events).hasSize(2);
        assertThat(events.getFirst()).containsEntry("body",
            "{\"password\":\"[REDACTED]\",\"displayName\":\"测试\"}");
        assertThat(events.getLast()).containsEntry("body", "{\"token\":\"[REDACTED]\",\"code\":200}");
        assertThat(events.toString()).doesNotContain("request-secret-canary", "response-secret-canary",
            "header-secret-canary");
        assertThat(((Map<?, ?>) events.getFirst().get("requestHeaders")).get("Authorization"))
            .isEqualTo(List.of("[REDACTED]"));
        assertThat(response.getContentAsString()).isEqualTo(responseBody);
        assertThat(response.getHeader("encrypt-key")).isNull();
    }

    @Test
    void restoresPreexistingMdcValue() throws Exception {
        MDC.put(SysLogFilter.MDC_REQUEST_ID, "outer-request-id");
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, ignored -> {
        });

        filter.doFilter(new MockHttpServletRequest("GET", "/mdc"), new MockHttpServletResponse(),
            (request, response) -> assertThat(MDC.get(SysLogFilter.MDC_REQUEST_ID))
                .isNotEqualTo("outer-request-id"));

        assertThat(MDC.get(SysLogFilter.MDC_REQUEST_ID)).isEqualTo("outer-request-id");
    }

    @Test
    void loggingFailureDoesNotChangeBusinessResponse() throws Exception {
        SysLogFilter filter = new SysLogFilter(1024, 2 * 1024 * 1024, event -> {
            throw new IllegalStateException("encoder unavailable");
        });
        MockHttpServletRequest request = jsonRequest("{\"value\":1}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) servletResponse;
            httpResponse.setStatus(202);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"ok\":true}");
        });

        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(response.getContentAsString()).isEqualTo("{\"ok\":true}");
        assertThat(response.getHeader(SysLogFilter.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(MDC.get(SysLogFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    void neverCopiesRawOAuthQueryAndRedirectIntoLogEvents() throws Exception {
        String canary = "credential-canary-query";
        List<Map<String, Object>> events = new ArrayList<>();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sso/oauth2/authorize");
        request.setQueryString("code=" + canary + "&page=1&page=2");
        request.addParameter("code", canary);
        request.addParameter("page", "1", "2");
        request.addHeader("Referer", "https://test.invalid/callback?code=" + canary);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SysLogFilter(1024, 2 * 1024 * 1024, events::add).doFilter(request, response, (req, res) -> {
            var httpResponse = (jakarta.servlet.http.HttpServletResponse) res;
            httpResponse.setContentType("application/json");
            httpResponse.setHeader("Location", "https://test.invalid/callback?code=" + canary);
            httpResponse.getWriter().write("{\"redirectUri\":\"https://test.invalid/callback?code=" + canary + "\"}");
        });

        assertThat(events.toString()).doesNotContain(canary);
        assertThat(((Map<?, ?>) events.getFirst().get("parameters")).get("page"))
            .isEqualTo(List.of("1", "2"));
        assertThat(response.getContentAsString()).contains(canary);
    }

    private MockHttpServletRequest jsonRequest(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private void assertThatCodeIsUuid(String value) {
        assertThat(value).isEqualTo(UUID.fromString(value).toString());
    }
}
