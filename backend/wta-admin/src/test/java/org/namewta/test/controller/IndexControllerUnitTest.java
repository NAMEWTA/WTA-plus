package org.namewta.test.controller;

import org.namewta.web.controller.IndexController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class IndexControllerUnitTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void returnsFixedStartupTimeAndCurrentAccessTime() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Instant startupInstant = Instant.parse("2026-08-26T01:02:03Z");
        when(applicationContext.getStartupDate()).thenReturn(startupInstant.toEpochMilli());
        IndexController controller = new IndexController(applicationContext);

        LocalDateTime beforeAccess = LocalDateTime.now().minusSeconds(1);
        String result = controller.index();
        LocalDateTime afterAccess = LocalDateTime.now().plusSeconds(1);

        String expectedStartupTime = DATE_TIME_FORMATTER.format(
            LocalDateTime.ofInstant(startupInstant, ZoneId.systemDefault()));
        String currentTime = result.substring(result.indexOf("当前时间为：") + "当前时间为：".length(), result.length() - 1);

        assertThat(result).startsWith("后端已经成功启动，启动的时间为：" + expectedStartupTime + "。当前时间为：");
        assertThat(result).endsWith("。");
        assertThat(LocalDateTime.parse(currentTime, DATE_TIME_FORMATTER))
            .isBetween(beforeAccess, afterAccess);
        assertThat(controller.index()).contains("启动的时间为：" + expectedStartupTime + "。");
    }

}
