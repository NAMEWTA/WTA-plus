package org.namewta.test.third.logging;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local")
class ThirdOutboundSysLogPersistenceTest {

    @TempDir
    Path logDirectory;

    @Test
    void recorderPersistsSanitizedBoundedAttemptsThroughProductionLogback() throws Exception {
        URL configuration = getClass().getClassLoader().getResource("logback-plus.xml");
        assertThat(configuration).isNotNull();
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Path launcher = launcherJar();
        Path processOutput = logDirectory.resolve("probe.out");
        ProcessBuilder builder = new ProcessBuilder(
            java,
            "-Dlogback.configurationFile=" + Path.of(configuration.toURI()),
            "-jar", launcher.toString());
        builder.environment().put("LOG_PATH", logDirectory.toString());
        builder.redirectErrorStream(true);
        builder.redirectOutput(processOutput.toFile());
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) process.destroyForcibly();
        assertThat(finished).isTrue();
        String output = Files.readString(processOutput, StandardCharsets.UTF_8);
        assertThat(process.exitValue()).as(output).isZero();

        Path logFile = logDirectory.resolve("sys-console.log");
        assertThat(logFile).isRegularFile();
        List<String> jsonLines = Files.readAllLines(logFile, StandardCharsets.UTF_8).stream()
            .filter(line -> line.startsWith("{"))
            .toList();
        assertThat(jsonLines).hasSize(2);
        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode start = mapper.readTree(jsonLines.getFirst());
        JsonNode finish = mapper.readTree(jsonLines.getLast());
        assertThat(start.path("事件类型").asText()).isEqualTo("THIRD_HTTP_ATTEMPT_START");
        assertThat(finish.path("事件类型").asText()).isEqualTo("THIRD_HTTP_ATTEMPT_FINISH");
        assertThat(start.path("请求标识").asText()).isEqualTo("third-log-canary");
        assertThat(finish.path("failureCategory").asText()).isEqualTo("NONE");
        assertThat(finish.path("httpStatus").asInt()).isEqualTo(200);

        String persisted = Files.readString(logFile, StandardCharsets.UTF_8);
        assertThat(persisted)
            .contains("***", "...[truncated]")
            .doesNotContain("Bearer raw-secret", "raw-api-key", "raw-query-secret",
                "request-secret", "response-secret");
        assertThat(Files.size(logFile)).isLessThan(40_000L);
    }

    private Path launcherJar() throws Exception {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, ThirdOutboundSysLogProcessProbe.class.getName());
        String classPath = Arrays.stream(System.getProperty("java.class.path").split(
                java.util.regex.Pattern.quote(System.getProperty("path.separator"))))
            .map(entry -> Path.of(entry).toAbsolutePath().normalize().toUri().toASCIIString())
            .reduce((left, right) -> left + " " + right)
            .orElseThrow();
        attributes.put(Attributes.Name.CLASS_PATH, classPath);
        Path launcher = logDirectory.resolve("third-log-probe.jar");
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(launcher), manifest)) {
            // The manifest owns the complete Surefire classpath; no classes are copied.
        }
        return launcher;
    }
}
