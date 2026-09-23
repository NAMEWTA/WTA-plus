package org.namewta.common.web.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class SysLogFailureRedactionTest {
    private static final String CANARY = "credential-canary-failure";

    @Test
    void sinkAndHandlerFailuresNeverPrintExceptionPayloadOrCause() throws Exception {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/failed");
            MockHttpServletResponse response = new MockHttpServletResponse();
            new SysLogFilter(1024, 2 * 1024 * 1024, event -> {
                throw new IllegalStateException(CANARY, new RuntimeException(CANARY));
            }).doFilter(request, response, (req, res) -> res.getWriter().write("unchanged"));
            new GlobalExceptionHandler().handleRuntimeException(new IllegalStateException(CANARY), request);
            assertThat(response.getContentAsString()).isEqualTo("unchanged");
            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain(CANARY);
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            root.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void exceptionLogsHideOnlineTokenWithServletContextButKeepOrdinaryPaths() {
        String token = "credential-canary-online-error";
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            for (String path : new String[]{"/monitor/online/" + token, "/monitor/online/myself/" + token,
                "/monitor/online/list"}) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app" + path);
                request.setContextPath("/app");
                new GlobalExceptionHandler().handleRuntimeException(new IllegalStateException(token), request);
                assertThat(request.getRequestURI()).isEqualTo("/app" + path);
            }
            var handlerLogs = appender.list.stream().filter(event ->
                GlobalExceptionHandler.class.getName().equals(event.getLoggerName())).toList();
            assertThat(handlerLogs).hasSize(3);
            assertThat(handlerLogs.get(0).getFormattedMessage()).contains("/app/monitor/online/[REDACTED]").doesNotContain(token);
            assertThat(handlerLogs.get(1).getFormattedMessage()).contains("/app/monitor/online/myself/[REDACTED]").doesNotContain(token);
            assertThat(handlerLogs.get(2).getFormattedMessage()).contains("/app/monitor/online/list");
        } finally {
            root.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void formRepeatsRemainVisibleWhileNestedSecretsAndOAuthCodeDisappear() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/context/sso/oauth2/token");
        request.setContextPath("/context");
        request.setContentType("application/x-www-form-urlencoded");
        request.addParameter("code", CANARY);
        request.addParameter("user[password]", CANARY);
        request.addParameter("page", "1", "2");
        new SysLogFilter(1024, 2 * 1024 * 1024, events::add).doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(req.getParameter("code")).isEqualTo(CANARY);
            assertThat(req.getParameterValues("page")).containsExactly("1", "2");
        });
        assertThat(events.toString()).doesNotContain(CANARY);
        assertThat(((Map<?, ?>) events.getFirst().get("parameters")).get("page")).isEqualTo(List.of("1", "2"));
    }

    @Test
    void malformedJsonHasOnlySafeSummaryAndOriginalBytesReachApplication() throws Exception {
        List<Map<String, Object>> events = new ArrayList<>();
        byte[] original = ("{\"password\":\"" + CANARY).getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/broken");
        request.setContentType("application/json");
        request.setContent(original);
        new SysLogFilter(1024, 2 * 1024 * 1024, events::add).doFilter(request, new MockHttpServletResponse(), (req, res) ->
            assertThat(req.getInputStream().readAllBytes()).containsExactly(original));
        assertThat(events.getFirst()).containsEntry("body", "[REDACTED]");
    }

    @Test
    void utf8PrefixNeverContainsPartialCodePoint() {
        SysLogBody body = SysLogBody.logged("ééé".getBytes(StandardCharsets.UTF_8), 6, 5);
        assertThat(body.body()).isEqualTo("éé");
        assertThat(body.truncated()).isTrue();
    }
}
