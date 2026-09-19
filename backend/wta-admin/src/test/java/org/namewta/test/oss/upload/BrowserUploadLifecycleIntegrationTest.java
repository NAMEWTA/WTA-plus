package org.namewta.test.oss.upload;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实 Chrome 文件字节写入一次性 MinIO；业务控制面故障由浏览器显式注入。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "browser.upload.integration", matches = "true")
class BrowserUploadLifecycleIntegrationTest {
    @TempDir
    Path temporary;

    @Test
    void uploadedObjectsSurvivePreviewFailureAndImportLifetimesTerminate() throws Exception {
        Path root = Path.of(System.getProperty("namewta.repo.root"));
        Path dist = root.resolve("frontend/apps/admin-web/dist");
        assertThat(dist.resolve("index.html")).isRegularFile();
        URI endpoint = URI.create(System.getProperty("oss.minio.integration.endpoint"));
        assertThat(endpoint.getHost()).isEqualTo("127.0.0.1");
        String bucket = "t18-browser-" + UUID.randomUUID().toString().replace("-", "");
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
            System.getProperty("oss.minio.integration.access-key", "namewta"),
            System.getProperty("oss.minio.integration.secret-key", "namewta123")));
        var configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        Map<String, byte[]> objects = Map.of(
            "owned.txt", "owned upload bytes".getBytes(StandardCharsets.UTF_8),
            "owned.png", Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9ioAAAAASUVORK5CYII="));
        try (S3Client storage = S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                 .credentialsProvider(credentials).serviceConfiguration(configuration).build();
             S3Presigner presigner = S3Presigner.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
                 .credentialsProvider(credentials).serviceConfiguration(configuration).build()) {
            storage.createBucket(request -> request.bucket(bucket));
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var executor = Executors.newSingleThreadExecutor();
            server.setExecutor(executor);
            server.createContext("/", exchange -> {
                String requestPath = exchange.getRequestURI().getPath();
                if (requestPath.startsWith("/prod-api/")) {
                    exchange.sendResponseHeaders(503, -1);
                    exchange.close();
                    return;
                }
                Path file = dist.resolve(requestPath.substring(1)).normalize();
                if (!file.startsWith(dist) || !Files.isRegularFile(file)) file = dist.resolve("index.html");
                String name = file.getFileName().toString();
                String contentType = name.endsWith(".js") ? "text/javascript" : name.endsWith(".css") ? "text/css"
                    : name.endsWith(".svg") ? "image/svg+xml" : name.endsWith(".html") ? "text/html" : "application/octet-stream";
                byte[] bytes = Files.readAllBytes(file);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
                finally { exchange.close(); }
            });
            Process browser = null;
            try {
                server.start();
                var command = new ProcessBuilder("corepack", "pnpm", "exec", "playwright", "test",
                    "--config", "playwright.upload.config.ts").directory(root.resolve("frontend").toFile());
                command.environment().put("UPLOAD_TEST_ORIGIN", "http://127.0.0.1:" + server.getAddress().getPort());
                for (String key : objects.keySet()) {
                    String suffix = key.endsWith(".png") ? "IMAGE" : "FILE";
                    String put = presigner.presignPutObject(request -> request.signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(object -> object.bucket(bucket).key(key))).url().toString();
                    String get = presigner.presignGetObject(request -> request.signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(object -> object.bucket(bucket).key(key))).url().toString();
                    command.environment().put("UPLOAD_TEST_PUT_" + suffix, put);
                    command.environment().put("UPLOAD_TEST_GET_" + suffix, get);
                }
                Path log = temporary.resolve("chrome.log");
                browser = command.redirectErrorStream(true).redirectOutput(log.toFile()).start();
                assertThat(browser.waitFor(240, TimeUnit.SECONDS)).as("owned Chrome terminates").isTrue();
                String output = Files.readString(log).replaceAll("([?&]X-Amz-[^=]+)=([^&\\s\"']+)", "$1=[REDACTED]");
                System.out.println(output);
                assertThat(browser.exitValue()).as("Chrome upload/import exit; details in test output").isZero();
                for (var object : objects.entrySet()) {
                    byte[] actual = storage.getObjectAsBytes(request -> request.bucket(bucket).key(object.getKey())).asByteArray();
                    assertThat(actual).as("persisted MinIO bytes for %s", object.getKey()).isEqualTo(object.getValue());
                }
                System.out.println("T-18: Chrome file and image bytes verified in owned MinIO after preview failures");
            } finally {
                if (browser != null && browser.isAlive()) browser.destroyForcibly().waitFor();
                server.stop(0);
                executor.shutdownNow();
                for (String key : objects.keySet()) storage.deleteObject(request -> request.bucket(bucket).key(key));
                storage.deleteBucket(request -> request.bucket(bucket));
            }
        }
    }
}
