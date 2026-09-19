package org.namewta.test.requestbody;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.http.CapturedRequestBody;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.openapi.annotation.OpenApi;
import org.namewta.common.openapi.config.properties.OpenApiProperties;
import org.namewta.common.openapi.gateway.OpenApiGatewayFilter;
import org.namewta.common.openapi.protocol.OpenApiCanonicalizer;
import org.namewta.common.openapi.protocol.OpenApiHeaders;
import org.namewta.common.openapi.protocol.OpenApiRequest;
import org.namewta.common.openapi.protocol.OpenApiSigner;
import org.namewta.common.openapi.registry.OpenApiAuthorizationMatcher;
import org.namewta.common.openapi.registry.OpenApiOperationRegistry;
import org.namewta.common.openapi.registry.SpringDocOperationSchemaResolver;
import org.namewta.common.openapi.session.OpenApiMachineSessionBridge;
import org.namewta.common.openapi.session.OpenApiMachineSessionOperations;
import org.namewta.common.openapi.session.VerifiedOpenApiIdentity;
import org.namewta.common.openapi.spi.OpenApiCredential;
import org.namewta.common.web.config.properties.XssProperties;
import org.namewta.common.web.filter.RepeatableFilter;
import org.namewta.common.web.filter.XssFilter;
import org.namewta.common.web.logging.SysLogFilter;
import org.namewta.system.api.model.LoginUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** 真实Jetty/MVC与生产Filter/签名实现；凭据、nonce和机器会话存储为确定性内存夹具。 */
@Tag("dev")
class RequestBodyHttpIntegrationTest {
    private static final int DEFAULT_LIMIT = 2 * 1024 * 1024;
    private static final Instant NOW = Instant.parse("2026-09-19T00:00:00Z");
    private static final String APP_KEY = "YXBwLWtleS13aXRoLTEyOC1iaXRzLW1pbmltdW0";
    private static final String APP_SECRET = "c2VjcmV0LXdpdGgtMjU2LWJpdHMtbWluaW11bS1rZXktbWF0ZXJpYWw";
    private static final OpenApiSigner SIGNER = new OpenApiSigner(new OpenApiCanonicalizer());

    @Test
    void ordinaryFixedAndChunkedRequestsRespectDefaultBoundary() throws Exception {
        try (Fixture fixture = new Fixture(DEFAULT_LIMIT, DEFAULT_LIMIT)) {
            for (boolean chunked : new boolean[]{false, true}) {
                for (int size : new int[]{DEFAULT_LIMIT - 1, DEFAULT_LIMIT, DEFAULT_LIMIT + 1}) {
                    int before = fixture.controller.calls.get();
                    var response = fixture.send("/body", json(size), chunked, false, false, "application/json");
                    assertEquals(size > DEFAULT_LIMIT ? 413 : 200, response.statusCode(), response.body());
                    assertEquals(before + (size > DEFAULT_LIMIT ? 0 : 1), fixture.controller.calls.get());
                    if (size <= DEFAULT_LIMIT) assertEquals(chunked, fixture.chunked.get());
                }
            }
        }
    }

    @Test
    void signedFixedAndChunkedRequestsRespectDefaultBoundary() throws Exception {
        try (Fixture fixture = new Fixture(DEFAULT_LIMIT, DEFAULT_LIMIT)) {
            for (boolean chunked : new boolean[]{false, true}) {
                for (int size : new int[]{DEFAULT_LIMIT - 1, DEFAULT_LIMIT, DEFAULT_LIMIT + 1}) {
                    int before = fixture.controller.calls.get();
                    var response = fixture.send("/machine", json(size), chunked, true, false, "application/json");
                    assertEquals(size > DEFAULT_LIMIT ? 413 : 200, response.statusCode(), response.body());
                    assertEquals(before + (size > DEFAULT_LIMIT ? 0 : 1), fixture.controller.calls.get());
                }
            }
        }
    }

    @Test
    void invalidSignatureWithLargeBodyIsRejectedBeforeBusinessForBothFramings() throws Exception {
        try (Fixture fixture = new Fixture(64, 128)) {
            for (boolean chunked : new boolean[]{false, true}) {
                assertEquals(413, fixture.send("/machine", json(129), chunked, true, true, "application/json").statusCode());
                assertEquals(401, fixture.send("/machine", json(32), chunked, true, true, "application/json").statusCode());
            }
            assertEquals(0, fixture.controller.calls.get());
        }
    }

    @Test
    void machineBudgetCanBeLargerOrSmallerThanOrdinaryBudget() throws Exception {
        try (Fixture fixture = new Fixture(64, 128)) {
            assertEquals(413, fixture.send("/body", json(96), false, false, false, "application/json").statusCode());
            assertEquals(200, fixture.send("/machine", json(96), true, true, false, "application/json").statusCode());
        }
        try (Fixture fixture = new Fixture(128, 64)) {
            assertEquals(200, fixture.send("/body", json(96), true, false, false, "application/json").statusCode());
            assertEquals(413, fixture.send("/machine", json(96), false, true, false, "application/json").statusCode());
        }
    }

    @Test
    void signatureUsesExactUtf8WhitespaceAndTagsBeforeTheIndependentXssView() throws Exception {
        try (Fixture fixture = new Fixture(128, 128)) {
            byte[] raw = " \r\n{\"data\":\"<b>中</b>\"} \n".getBytes(StandardCharsets.UTF_8);
            var response = fixture.send("/machine", raw, true, true, false, "application/problem+json");
            assertEquals(200, response.statusCode(), response.body());
            Map<String, Object> result = JsonUtils.parseMap(response.body());
            assertEquals(hash(raw), result.get("rawHash"));
            assertEquals(hash("{\"data\":\"中\"}".getBytes(StandardCharsets.UTF_8)), result.get("viewHash"));
        }
    }

    @Test
    void loggingFallbackAndStructuredJsonSuffixCannotBypassLimit() throws Exception {
        try (Fixture fixture = new Fixture(64, 128)) {
            for (String contentType : new String[]{"text/plain", "application/problem+json"}) {
                assertEquals(413, fixture.send("/body", json(65), true, false, false, contentType).statusCode());
            }
            assertEquals(0, fixture.controller.calls.get());
        }
    }

    @Test
    void ordinaryUploadRemainsStreamingBeyondTheJsonBudget() throws Exception {
        try (Fixture fixture = new Fixture(64, 128)) {
            byte[] content = ("--owned\r\nContent-Disposition: form-data; name=\"file\"; filename=\"data.txt\"\r\n\r\n"
                + "x".repeat(DEFAULT_LIMIT) + "\r\n--owned--\r\n").getBytes(StandardCharsets.UTF_8);
            var response = fixture.send("/upload", content, true, false, false, "multipart/form-data; boundary=owned");
            assertEquals(200, response.statusCode(), response.body());
            assertEquals(Integer.toString(content.length), response.body());
            assertFalse(fixture.uploadCached.get());
        }
    }

    @Test
    void cancelledJsonReadDoesNotRunBusinessOrPoisonTheNextRequest() throws Exception {
        try (Fixture fixture = new Fixture(1024, 1024)) {
            try (Socket socket = new Socket("127.0.0.1", fixture.port)) {
                socket.getOutputStream().write(("POST /body HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\n"
                    + "Content-Length: 1000\r\n\r\n{\"data\":\"short").getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                assertTrue(fixture.started.await(5, TimeUnit.SECONDS));
            }
            assertTrue(fixture.finished.await(5, TimeUnit.SECONDS));
            assertEquals(0, fixture.controller.calls.get());
            assertEquals(200, fixture.send("/body", json(32), false, false, false, "application/json").statusCode());
        }
    }

    @Test
    void cancelledUploadReleasesTheHandlerWithoutInstallingABodyCache() throws Exception {
        try (Fixture fixture = new Fixture(64, 128)) {
            try (Socket socket = new Socket("127.0.0.1", fixture.port)) {
                socket.getOutputStream().write(("POST /upload HTTP/1.1\r\nHost: localhost\r\n"
                    + "Content-Type: multipart/form-data; boundary=owned\r\nContent-Length: 1000\r\n\r\n--owned\r\n")
                    .getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                assertTrue(fixture.started.await(5, TimeUnit.SECONDS));
            }
            assertTrue(fixture.finished.await(5, TimeUnit.SECONDS));
            assertFalse(fixture.uploadCached.get());
            assertEquals(200, fixture.send("/upload", new byte[128], true, false, false,
                "application/octet-stream").statusCode());
        }
    }

    @Test
    void sseFlushesBeforeCompletionAndStopsOnClientCancellation() throws Exception {
        try (Fixture fixture = new Fixture(32, 32)) {
            try (Socket socket = new Socket("127.0.0.1", fixture.port)) {
                socket.setSoTimeout(5000);
                socket.getOutputStream().write("GET /events HTTP/1.1\r\nHost: localhost\r\nAccept: text/event-stream\r\n\r\n"
                    .getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                var reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                boolean received = false;
                for (int i = 0; i < 30; i++) {
                    String line = reader.readLine();
                    if (line != null && line.startsWith("data:")) { received = true; break; }
                }
                assertTrue(received, "SSE must flush before the response completes");
                assertEquals(1, fixture.streamStopped.getCount());
            }
            assertTrue(fixture.streamStopped.await(5, TimeUnit.SECONDS));
            assertTrue(fixture.streamDisconnected.get());
        }
    }

    @Test
    void concurrentBodiesRemainIsolatedWhileRawAndXssViewsAreHeld() throws Exception {
        int concurrency = Integer.getInteger("namewta.body-probe.concurrency", 4);
        int size = Integer.getInteger("namewta.body-probe.bytes", 64 * 1024);
        assertTrue(concurrency > 0 && concurrency <= 16);
        assertTrue(size >= 32 && size <= DEFAULT_LIMIT);
        var memory = java.lang.management.ManagementFactory.getMemoryMXBean();
        var collectors = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
        long gcCount = collectors.stream().mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum();
        long gcMillis = collectors.stream().mapToLong(bean -> Math.max(0, bean.getCollectionTime())).sum();
        long baseline = memory.getHeapMemoryUsage().getUsed();
        var peak = new java.util.concurrent.atomic.AtomicLong(baseline);
        var sampler = Executors.newSingleThreadScheduledExecutor();
        long started = System.nanoTime();
        long heldHeap = 0;
        try (Fixture fixture = new Fixture(DEFAULT_LIMIT, DEFAULT_LIMIT);
             var clients = Executors.newVirtualThreadPerTaskExecutor()) {
            sampler.scheduleAtFixedRate(() -> peak.accumulateAndGet(memory.getHeapMemoryUsage().getUsed(), Math::max),
                0, 5, TimeUnit.MILLISECONDS);
            for (int round = 0; round < 2; round++) {
                CountDownLatch arrived = new CountDownLatch(concurrency);
                CountDownLatch release = new CountDownLatch(1);
                fixture.controller.arrived = arrived;
                fixture.controller.release = release;
                var requests = new java.util.ArrayList<java.util.concurrent.Future<HttpResponse<String>>>();
                var hashes = new java.util.ArrayList<String>();
                try {
                    for (int i = 0; i < concurrency; i++) {
                        byte[] bytes = json(size);
                        bytes[9] = (byte) ('a' + i + round);
                        hashes.add(hash(bytes));
                        boolean signed = i % 2 == 0;
                        boolean chunked = i % 3 == 0;
                        requests.add(clients.submit(() -> fixture.send(signed ? "/machine" : "/body",
                            bytes, chunked, signed, false, "application/json")));
                    }
                    assertTrue(arrived.await(10, TimeUnit.SECONDS), "all request bodies must be held simultaneously");
                    heldHeap = Math.max(heldHeap, memory.getHeapMemoryUsage().getUsed());
                } finally {
                    release.countDown();
                }
                for (int i = 0; i < requests.size(); i++) {
                    var response = requests.get(i).get(10, TimeUnit.SECONDS);
                    assertEquals(200, response.statusCode(), response.body());
                    var result = JsonUtils.parseMap(response.body());
                    assertEquals(hashes.get(i), result.get("rawHash"));
                    assertEquals(hashes.get(i), result.get("viewHash"));
                }
            }
            assertEquals(2 * concurrency, fixture.controller.calls.get());
        } finally {
            sampler.shutdownNow();
            assertTrue(sampler.awaitTermination(5, TimeUnit.SECONDS));
        }
        String reportPath = System.getProperty("namewta.body-probe.report");
        if (reportPath != null) {
            Map<String, Object> report = new java.util.LinkedHashMap<>();
            report.put("scope", "single JVM including Jetty, Spring, HTTP client, signature and XSS; not production capacity");
            report.put("java", System.getProperty("java.version"));
            report.put("processors", Runtime.getRuntime().availableProcessors());
            report.put("heapMaxBytes", memory.getHeapMemoryUsage().getMax());
            report.put("concurrency", concurrency);
            report.put("requestBytes", size);
            report.put("logPrefixBytes", Integer.getInteger("namewta.body-probe.log-bytes", 32));
            report.put("requests", 2 * concurrency);
            report.put("baselineHeapBytes", baseline);
            report.put("sampledPeakHeapBytes", peak.get());
            report.put("heldHeapBytes", heldHeap);
            report.put("gcCollections", collectors.stream().mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum() - gcCount);
            report.put("gcMillis", collectors.stream().mapToLong(bean -> Math.max(0, bean.getCollectionTime())).sum() - gcMillis);
            report.put("elapsedMillis", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            java.nio.file.Files.writeString(java.nio.file.Path.of(reportPath), JsonUtils.toJsonString(report));
            long expectedHeap = Long.getLong("namewta.body-probe.expected-heap", 0L);
            if (expectedHeap > 0) {
                assertTrue(memory.getHeapMemoryUsage().getMax() <= expectedHeap,
                    "the probe JVM must actually use the requested heap ceiling");
            }
        }
    }

    static byte[] json(int length) {
        byte[] bytes = ("{\"data\":\"" + "x".repeat(length - 11) + "\"}").getBytes(StandardCharsets.UTF_8);
        assertEquals(length, bytes.length);
        return bytes;
    }

    static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    @Configuration
    @EnableWebMvc
    static class WebConfiguration {
        @Bean BodyController bodyController() { return new BodyController(); }
    }

    @RestController
    static class BodyController {
        final AtomicInteger calls = new AtomicInteger();
        volatile CountDownLatch arrived;
        volatile CountDownLatch release;
        @PostMapping(value = "/body", produces = "application/json")
        String body(@RequestBody byte[] bytes, HttpServletRequest request) { return result(bytes, request); }
        @OpenApi("Bounded exact body")
        @SaCheckPermission("body:read")
        @PostMapping(value = "/machine", produces = "application/json")
        String machine(@RequestBody byte[] bytes, HttpServletRequest request) { return result(bytes, request); }
        private String result(byte[] bytes, HttpServletRequest request) {
            calls.incrementAndGet();
            if (arrived != null) {
                arrived.countDown();
                try {
                    if (!release.await(12, TimeUnit.SECONDS)) throw new IllegalStateException("owned body probe timed out");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("owned body probe interrupted", exception);
                }
            }
            return JsonUtils.toJsonString(Map.of("rawHash", hash(CapturedRequestBody.find(request).copy()),
                "viewHash", hash(bytes), "length", bytes.length));
        }
    }

    static final class Fixture implements AutoCloseable {
        final Server server = new Server(new InetSocketAddress("127.0.0.1", 0));
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        final java.util.concurrent.ScheduledExecutorService streamWriter = Executors.newSingleThreadScheduledExecutor();
        final AtomicBoolean chunked = new AtomicBoolean();
        final AtomicBoolean uploadCached = new AtomicBoolean();
        final AtomicBoolean streamDisconnected = new AtomicBoolean();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch finished = new CountDownLatch(1);
        final CountDownLatch streamStopped = new CountDownLatch(1);
        final BodyController controller;
        final int port;

        Fixture(int ordinaryLimit, int machineLimit) throws Exception {
            try {
                ServletContextHandler handler = new ServletContextHandler();
                handler.setContextPath("/");
                context.setServletContext(handler.getServletContext());
                context.register(WebConfiguration.class);
                context.refresh();
                controller = context.getBean(BodyController.class);
                var mappings = context.getBean(RequestMappingHandlerMapping.class);
                var registry = new OpenApiOperationRegistry(mappings, new SpringDocOperationSchemaResolver());
                registry.afterPropertiesSet();
                var sessions = new Sessions();
                var bridge = new OpenApiMachineSessionBridge(userId -> sessions.user(), sessions, Duration.ofHours(1));
                OpenApiProperties properties = new OpenApiProperties();
                properties.setMaxBodySize(DataSize.ofBytes(machineLimit));
                Set<String> nonces = ConcurrentHashMap.newKeySet();
                var gateway = new OpenApiGatewayFilter(mappings, registry,
                    key -> new OpenApiCredential(7L, 9L, APP_KEY, APP_SECRET, NOW.plusSeconds(3600)), SIGNER,
                    (key, nonce, ttl) -> nonces.add(key + nonce), (scope, limit, interval) -> true,
                    bridge, new OpenApiAuthorizationMatcher(), event -> { }, properties,
                    Clock.fixed(NOW, ZoneOffset.UTC), sessions.current::get);
                add(handler, (request, response, chain) -> {
                    chunked.set(request.getContentLengthLong() < 0);
                    started.countDown();
                    try { chain.doFilter(request, response); } finally { finished.countDown(); }
                });
                add(handler, gateway);
                add(handler, new RepeatableFilter(ordinaryLimit));
                add(handler, new SysLogFilter(Integer.getInteger("namewta.body-probe.log-bytes", 32), ordinaryLimit, event -> { }));
                add(handler, new XssFilter(new XssProperties(), ordinaryLimit));
                handler.addServlet(new ServletHolder(new DispatcherServlet(context)), "/");
                handler.addServlet(new ServletHolder(new HttpServlet() {
                    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        uploadCached.set(CapturedRequestBody.find(request) != null);
                        long count = request.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
                        response.getWriter().write(Long.toString(count));
                    }
                }), "/upload");
                ServletHolder events = new ServletHolder(new HttpServlet() {
                    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                        response.setContentType("text/event-stream");
                        var async = request.startAsync(request, response);
                        async.setTimeout(8000);
                        AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();
                        future.set(streamWriter.scheduleAtFixedRate(() -> {
                            try {
                                response.getOutputStream().write(("data: " + "x".repeat(1024) + "\n\n").getBytes(StandardCharsets.UTF_8));
                                response.getOutputStream().flush();
                            } catch (IOException failure) {
                                streamDisconnected.set(true);
                                ScheduledFuture<?> task = future.get();
                                if (task != null) task.cancel(false);
                                try { async.complete(); } finally { streamStopped.countDown(); }
                            }
                        }, 10, 10, TimeUnit.MILLISECONDS));
                    }
                });
                events.setAsyncSupported(true);
                handler.addServlet(events, "/events");
                server.setHandler(handler);
                server.start();
                port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            } catch (Exception failure) {
                close();
                throw failure;
            }
        }

        private static void add(ServletContextHandler handler, Filter filter) {
            FilterHolder holder = new FilterHolder(filter);
            holder.setAsyncSupported(true);
            handler.addFilter(holder, "/*", EnumSet.of(DispatcherType.REQUEST));
        }

        HttpResponse<String> send(String path, byte[] bytes, boolean chunked, boolean signed, boolean invalid, String type) throws Exception {
            var publisher = chunked ? HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(bytes))
                : HttpRequest.BodyPublishers.ofByteArray(bytes);
            var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", type).POST(publisher);
            if (signed) {
                String timestamp = Long.toString(NOW.getEpochSecond());
                String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                String signature = SIGNER.sign(new OpenApiRequest(APP_KEY, timestamp, nonce, "POST", path, "",
                    invalid ? new byte[]{1} : bytes), APP_SECRET);
                builder.header(OpenApiHeaders.VERSION, "v1").header(OpenApiHeaders.APP_KEY, APP_KEY)
                    .header(OpenApiHeaders.TIMESTAMP, timestamp).header(OpenApiHeaders.NONCE, nonce)
                    .header(OpenApiHeaders.SIGNATURE, signature);
            }
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }

        @Override public void close() throws Exception {
            streamWriter.shutdownNow();
            try { server.stop(); } finally { context.close(); client.close(); }
            assertTrue(streamWriter.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    static final class Sessions implements OpenApiMachineSessionOperations {
        final ThreadLocal<LoginUser> current = new ThreadLocal<>();
        private LoginUser stored;
        LoginUser user() {
            LoginUser user = new LoginUser(); user.setUserId(9L); user.setUserType("openapi");
            user.setMenuPermission(Set.of("body:read")); user.setRolePermission(Set.of()); return user;
        }
        @Override public LoginUser find(VerifiedOpenApiIdentity identity) { return stored; }
        @Override public void create(VerifiedOpenApiIdentity identity, LoginUser user, Duration ttl) { stored = user; }
        @Override public <T> T inRequestScope(VerifiedOpenApiIdentity identity, Supplier<T> callback) {
            current.set(stored); try { return callback.get(); } finally { current.remove(); }
        }
        @Override public <T> T withUserLock(Long userId, Supplier<T> callback) { return callback.get(); }
        @Override public int invalidateByUserId(Long userId) { stored = null; return 1; }
    }
}
