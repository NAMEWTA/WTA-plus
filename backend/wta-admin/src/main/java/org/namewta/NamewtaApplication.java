package org.namewta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 启动程序
 */

@SpringBootApplication
public class NamewtaApplication {

    private static final Logger log = LoggerFactory.getLogger(NamewtaApplication.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long MEGABYTE = 1024L * 1024L;
    private static final long GIGABYTE = MEGABYTE * 1024L;

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        long startupStart = System.nanoTime();
        SpringApplication application = new SpringApplication(NamewtaApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        ConfigurableApplicationContext applicationContext = application.run(args);
        printRuntimeInfo(applicationContext, System.nanoTime() - startupStart);
    }

    static void printRuntimeInfo(ApplicationContext applicationContext, long startupDurationNanos) {
        log.info("\n{}", buildRuntimeInfo(applicationContext, startupDurationNanos));
    }

    static String buildRuntimeInfo(ApplicationContext applicationContext, long startupDurationNanos) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return """
            ==================
            访问地址：%s
            启动时间：%s
            运行的系统：%s
            启动耗时：%s
            运行内存：已用 %s / 最大 %s
            ==================""".formatted(
            getAccessAddress(applicationContext),
            getStartupTime(applicationContext),
            getOperatingSystem(),
            formatStartupDuration(startupDurationNanos),
            formatMemory(usedMemory),
            formatMemory(runtime.maxMemory()));
    }

    private static String getStartupTime(ApplicationContext applicationContext) {
        Instant startupInstant = Instant.ofEpochMilli(applicationContext.getStartupDate());
        return DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(startupInstant, ZoneId.systemDefault()));
    }

    private static String getAccessAddress(ApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        String scheme = environment.getProperty("server.ssl.enabled", Boolean.class, false) ? "https" : "http";
        String host = environment.getProperty("server.address", "localhost");
        if ("0.0.0.0".equals(host) || "::".equals(host)) {
            host = "localhost";
        } else if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        int port = environment.getProperty("server.port", Integer.class, 8080);
        if (applicationContext instanceof WebServerApplicationContext webApplicationContext) {
            port = webApplicationContext.getWebServer().getPort();
        }
        String contextPath = environment.getProperty("server.servlet.context-path", "/");
        if (contextPath == null || contextPath.isBlank()) {
            contextPath = "/";
        } else if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        return "%s://%s:%d%s".formatted(scheme, host, port, contextPath);
    }

    private static String getOperatingSystem() {
        return "%s %s（%s）".formatted(
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"));
    }

    private static String formatStartupDuration(long startupDurationNanos) {
        double startupDurationSeconds = startupDurationNanos / 1_000_000_000D;
        return String.format(Locale.ROOT, "%.2f 秒", startupDurationSeconds);
    }

    private static String formatMemory(long bytes) {
        if (bytes >= GIGABYTE) {
            return String.format(Locale.ROOT, "%.2f GB", bytes / (double) GIGABYTE);
        }
        return String.format(Locale.ROOT, "%.2f MB", bytes / (double) MEGABYTE);
    }

}
