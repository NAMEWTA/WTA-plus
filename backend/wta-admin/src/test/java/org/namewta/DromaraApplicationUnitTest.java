package org.namewta;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.core.env.Environment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class DromaraApplicationUnitTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void logsStartupSummaryWithActualAccessAddressAndStartupTime() {
        WebServerApplicationContext applicationContext = mock(WebServerApplicationContext.class);
        Environment environment = mock(Environment.class);
        WebServer webServer = mock(WebServer.class);
        Instant startupInstant = Instant.parse("2026-08-26T01:02:03Z");
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(applicationContext.getStartupDate()).thenReturn(startupInstant.toEpochMilli());
        when(applicationContext.getWebServer()).thenReturn(webServer);
        when(webServer.getPort()).thenReturn(9090);
        when(environment.getProperty("server.ssl.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("server.address", "localhost")).thenReturn("0.0.0.0");
        when(environment.getProperty("server.port", Integer.class, 8080)).thenReturn(0);
        when(environment.getProperty("server.servlet.context-path", "/")).thenReturn("api");

        Logger logger = (Logger) LoggerFactory.getLogger(DromaraApplication.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            DromaraApplication.printRuntimeInfo(applicationContext, 2_500_000_000L);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }

        String expectedStartupTime = DATE_TIME_FORMATTER.format(
            LocalDateTime.ofInstant(startupInstant, ZoneId.systemDefault()));
        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                .startsWith("\n==================")
                .contains("访问地址：https://localhost:9090/api")
                .contains("启动时间：" + expectedStartupTime)
                .contains("运行的系统：")
                .contains("启动耗时：2.50 秒")
                .contains("运行内存：已用 ")
                .endsWith("==================");
        });
    }

}
