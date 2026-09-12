package org.namewta.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 首页
 *
 * @author Lion Li
 */
@SaIgnore
@RestController
public class IndexController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String startupTime;

    public IndexController(ApplicationContext applicationContext) {
        this.startupTime = formatDateTime(applicationContext.getStartupDate());
    }

    /**
     * 访问首页时返回后端启动时间和当前访问时间。
     *
     * @return 后端运行时间信息
     */
    @GetMapping("/")
    public String index() {
        return "后端已经成功启动，启动的时间为：" + startupTime
            + "。当前时间为：" + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + "。";
    }

    private static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMATTER.format(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
    }

}
