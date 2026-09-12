package org.namewta.common.web.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class SysLogEventWriterTest {

    @Test
    void emitsOneParseableChineseJsonMessageAndPreservesBodyValue() {
        JsonMapper mapper = JsonMapper.builder().build();
        Logger logger = (Logger) LoggerFactory.getLogger(SysLogEventWriter.LOGGER_NAME);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("event", "HTTP_REQUEST");
            event.put("requestId", "request-id");
            event.put("bodyLogged", true);
            event.put("body", "line1\n\"quoted\"\u0001");

            new SysLogEventWriter(mapper).write(event);

            assertThat(appender.list).hasSize(1);
            String message = appender.list.getFirst().getFormattedMessage();
            assertThat(message).doesNotContain("\n");
            JsonNode parsed = mapper.readTree(message);
            assertThat(parsed.get("事件类型").asText()).isEqualTo("请求进入");
            assertThat(parsed.get("请求标识").asText()).isEqualTo("request-id");
            assertThat(parsed.get("正文已记录").asBoolean()).isTrue();
            assertThat(parsed.get("正文").asText()).isEqualTo("line1\n\"quoted\"\u0001");
            assertThat(parsed.has("event")).isFalse();
            assertThat(parsed.has("body")).isFalse();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }

    @Test
    void translatesResponseEventAndBodyOmissionReason() {
        JsonMapper mapper = JsonMapper.builder().build();
        Logger logger = (Logger) LoggerFactory.getLogger(SysLogEventWriter.LOGGER_NAME);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("event", "HTTP_RESPONSE");
            event.put("status", 200);
            event.put("bodyLogged", false);
            event.put("bodyOmissionReason", "NON_TEXT_CONTENT_TYPE");

            new SysLogEventWriter(mapper).write(event);

            JsonNode parsed = mapper.readTree(appender.list.getFirst().getFormattedMessage());
            assertThat(parsed.get("事件类型").asText()).isEqualTo("响应返回");
            assertThat(parsed.get("响应状态").asInt()).isEqualTo(200);
            assertThat(parsed.get("正文已记录").asBoolean()).isFalse();
            assertThat(parsed.get("正文省略原因").asText()).isEqualTo("非文本内容");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}
