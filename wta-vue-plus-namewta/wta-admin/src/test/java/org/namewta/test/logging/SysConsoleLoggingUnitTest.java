package org.namewta.test.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.status.Status;
import org.namewta.common.web.logging.SysLogEventWriter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class SysConsoleLoggingUnitTest {

    @TempDir
    Path logDirectory;

    @Test
    void writesSingleHttpJsonLineAndKeepsOnlySynchronousSysConsoleFile() throws Exception {
        Path oldInfo = logDirectory.resolve("sys-info.legacy.log");
        Path oldError = logDirectory.resolve("sys-error.legacy.log");
        Files.writeString(oldInfo, "keep-info", StandardCharsets.UTF_8);
        Files.writeString(oldError, "keep-error", StandardCharsets.UTF_8);

        URL configuration = getClass().getClassLoader().getResource("logback-plus.xml");
        assertThat(configuration).isNotNull();
        String xml = Files.readString(Path.of(configuration.toURI()), StandardCharsets.UTF_8);
        assertThat(xml).doesNotContain("file_info", "file_error", "async_info", "async_error", "AsyncAppender");
        assertThat(xml)
            .contains("<fileNamePattern>${log.path}/sys-console.%d{yyyy-MM-dd}.log.gz</fileNamePattern>")
            .contains("<maxHistory>60</maxHistory>")
            .contains("<totalSizeCap>40GB</totalSizeCap>");

        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        context.putProperty("LOG_PATH", logDirectory.toString());
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(configuration);
            context.start();

            assertAppenderAndRollingPolicy(context);
            assertDedicatedLogger(context);

            JsonMapper mapper = JsonMapper.builder().build();
            String originalBody = "line1\n\"quoted\"\u0001";
            String httpEvent = mapper.writeValueAsString(java.util.Map.of(
                "事件类型", "响应返回",
                "请求标识", "request-id",
                "正文", originalBody));

            context.getLogger(SysLogEventWriter.LOGGER_NAME).info(httpEvent);
            context.getLogger("example.normal").info("normal marker");
            assertNoLogbackErrors(context);
        } finally {
            context.stop();
        }

        List<String> lines = Files.readAllLines(logDirectory.resolve("sys-console.log"), StandardCharsets.UTF_8);
        assertThat(lines).hasSize(2);
        JsonNode event = JsonMapper.builder().build().readTree(lines.getFirst());
        assertThat(event.get("事件类型").asText()).isEqualTo("响应返回");
        assertThat(event.get("请求标识").asText()).isEqualTo("request-id");
        assertThat(event.get("正文").asText()).isEqualTo("line1\n\"quoted\"\u0001");
        assertThat(lines.getFirst()).startsWith("{").endsWith("}");
        assertThat(lines.getLast())
            .contains("[main] INFO ", "example.normal - normal marker")
            .doesNotStartWith("{");

        assertThat(Files.readString(oldInfo, StandardCharsets.UTF_8)).isEqualTo("keep-info");
        assertThat(Files.readString(oldError, StandardCharsets.UTF_8)).isEqualTo("keep-error");
        try (Stream<Path> files = Files.list(logDirectory)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                .containsExactlyInAnyOrder("sys-console.log", "sys-info.legacy.log", "sys-error.legacy.log");
        }
    }

    private void assertAppenderAndRollingPolicy(LoggerContext context) {
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        List<Appender<ILoggingEvent>> appenders = new ArrayList<>();
        root.iteratorForAppenders().forEachRemaining(appenders::add);

        assertThat(appenders).extracting(Appender::getName).containsExactly("console", "file_console");
        assertThat(root.getAppender("file_console")).isInstanceOf(RollingFileAppender.class);

        @SuppressWarnings("unchecked")
        RollingFileAppender<ILoggingEvent> fileAppender =
            (RollingFileAppender<ILoggingEvent>) root.getAppender("file_console");
        assertThat(Path.of(fileAppender.getFile())).isEqualTo(logDirectory.resolve("sys-console.log"));
        assertThat(fileAppender.isStarted()).isTrue();
        assertThat(fileAppender.getRollingPolicy()).isInstanceOf(TimeBasedRollingPolicy.class);

        TimeBasedRollingPolicy<?> rollingPolicy = (TimeBasedRollingPolicy<?>) fileAppender.getRollingPolicy();
        assertThat(rollingPolicy.getFileNamePattern()).endsWith("sys-console.%d{yyyy-MM-dd}.log.gz");
        assertThat(rollingPolicy.getMaxHistory()).isEqualTo(60);
    }

    private void assertDedicatedLogger(LoggerContext context) {
        Logger httpLogger = context.getLogger(SysLogEventWriter.LOGGER_NAME);
        assertThat(httpLogger.getLevel()).isEqualTo(Level.INFO);
        assertThat(httpLogger.isAdditive()).isTrue();
        assertThat(httpLogger.iteratorForAppenders().hasNext()).isFalse();

        context.getLogger("org.namewta").setLevel(Level.WARN);
        assertThat(httpLogger.isInfoEnabled()).isTrue();
    }

    private void assertNoLogbackErrors(LoggerContext context) {
        assertThat(context.getStatusManager().getCopyOfStatusList())
            .noneMatch(status -> status.getLevel() == Status.ERROR);
    }
}
