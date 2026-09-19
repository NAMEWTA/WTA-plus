package org.namewta.test.security;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.namewta.common.core.utils.NetUtils;
import org.namewta.common.core.utils.ServletUtils;
import org.namewta.common.core.utils.SpringUtils;
import org.namewta.common.core.utils.ip.ClientAddressResolver;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.aspect.LogAspect;
import org.namewta.common.log.event.OperLogEvent;
import org.namewta.common.redis.annotation.RateLimiter;
import org.namewta.common.redis.aspectj.RateLimiterAspect;
import org.namewta.common.redis.enums.LimitType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.security.config.SecurityConfig;
import org.namewta.common.security.config.properties.SecurityProperties;
import org.namewta.common.web.filter.ClientAddressFilter;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** 隔离Nginx单/双代理 -> 真实Jetty -> 生产来源解析、白名单、限流键与OperLogEvent。 */
@Tag("dev")
class TrustedClientAddressIntegrationTest {
    @TempDir
    Path directory;
    private final List<OperLogEvent> operations = Collections.synchronizedList(new ArrayList<>());
    private final List<String> socketPeers = Collections.synchronizedList(new ArrayList<>());
    private final String nginxImage = "nginx:1.31.1@sha256:608a100c71651bf5b773c89083b4a1ad7ef4b2bd05d7a7e552271e03123692ad";

    @Test
    void forgedHeadersCannotChangeAuthorizationRateIdentityOrAuditAcrossProxyChains() throws Exception {
        assumeTrue(Boolean.getBoolean("client.address.nginx.integration"), "isolated Docker Nginx required");
        Path repo = Path.of(System.getProperty("namewta.repo.root")).toAbsolutePath();
        var server = new Server(new InetSocketAddress("::", 0));
        try (var context = new AnnotationConfigApplicationContext(); var client = HttpClient.newHttpClient()) {
            context.registerBean(SpringUtils.class);
            context.addApplicationListener(event -> {
                if (event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof OperLogEvent operation) {
                    operations.add(operation);
                }
            });
            context.refresh();
            var handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.addFilter(new FilterHolder((request, response, chain) -> {
                socketPeers.add(request.getRemoteAddr());
                chain.doFilter(request, response);
            }), "/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addFilter(new FilterHolder(new ClientAddressFilter(new ClientAddressResolver(
                List.of("127.0.0.2/32", "127.0.0.3/32")))), "/*", EnumSet.of(DispatcherType.REQUEST));
            handler.addServlet(new ServletHolder(new ProbeServlet()), "/*");
            server.setHandler(handler);
            server.start();
            int backendPort = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            probe(client, "http://127.0.0.1:" + backendPort + "/probe", "127.0.0.1", false, "192.0.2.77");
            probe(client, "http://[::1]:" + backendPort + "/probe", "::1", false, "192.0.2.77");
            int beforeInvalid = operations.size();
            try (Socket socket = new Socket()) {
                socket.bind(new InetSocketAddress("127.0.0.2", 0));
                socket.connect(new InetSocketAddress("127.0.0.1", backendPort));
                socket.setSoTimeout(5000);
                socket.getOutputStream().write(("GET /probe HTTP/1.1\r\nHost: localhost\r\n"
                    + "X-Forwarded-For: 192.0.2.77,unknown\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                assertThat(new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .startsWith("HTTP/1.1 400");
            }
            assertThat(operations).hasSize(beforeInvalid);

            for (boolean doubleProxy : new boolean[]{false, true}) {
                int outerPort = unusedPort();
                int innerPort = unusedPort();
                String lb = template(repo, "lb/nginx-lb-http.conf.template")
                    .replace("listen 80 default_server;", "listen 127.0.0.1:" + outerPort
                        + ";\n    listen [::1]:" + outerPort + ";\n    proxy_bind " + (doubleProxy ? "127.0.0.3" : "127.0.0.2") + ";")
                    .replace("namewta-nginx-admin-web:80", "127.0.0.1:" + (doubleProxy ? innerPort : backendPort));
                lb = substituteUnusedUpstreams(lb, backendPort);
                String app = "";
                if (doubleProxy) {
                    app = template(repo, "apps/nginx-admin-web.conf.template")
                        .replaceFirst("(?s)map \\$http_upgrade.*?\\n}\\n", "")
                        .replace("listen 80 default_server;", "listen 127.0.0.1:" + innerPort + ";\n    proxy_bind 127.0.0.2;")
                        .replace("${BACKEND_SERVER1}", "127.0.0.1:" + backendPort)
                        .replace("    server ${BACKEND_SERVER2} max_fails=3 fail_timeout=10s;", "");
                }
                Files.writeString(directory.resolve("nginx.conf"), config(lb + app));
                String container = "namewta-client-ip-" + UUID.randomUUID();
                try {
                    docker("run", "--rm", "--network", "host", "-v", directory + ":/test:ro", nginxImage,
                        "nginx", "-t", "-c", "/test/nginx.conf");
                    docker("run", "-d", "--name", container, "--label", "namewta.test.owner=T-04", "--network", "host",
                        "-v", directory + ":/test:ro", nginxImage, "nginx", "-g", "daemon off;", "-c", "/test/nginx.conf");
                    awaitPort(outerPort);
                    for (String host : List.of("127.0.0.1", "[::1]")) {
                        String expected = host.replace("[", "").replace("]", "");
                        String url = "http://" + host + ":" + outerPort + "/console/prod-api/probe";
                        probe(client, url, expected, doubleProxy, null);
                        probe(client, url, expected, doubleProxy, "192.0.2.77");
                        probe(client, url, expected, doubleProxy, "unknown,192.0.2.77");
                    }
                } finally {
                    docker("rm", "-f", container);
                }
            }
            // TLS模板的来源合同由相同的真实Nginx解析器检查；HTTP请求矩阵在上方完整执行。
            command(List.of("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
                "-subj", "/CN=localhost", "-keyout", directory.resolve("privkey.pem").toString(),
                "-out", directory.resolve("fullchain.pem").toString()));
            String tls = substituteUnusedUpstreams(template(repo, "lb/nginx-lb-tls.conf.template"), backendPort)
                .replace("namewta-nginx-admin-web:80", "127.0.0.1:" + backendPort)
                .replace("listen 443 ssl default_server;", "listen 127.0.0.1:" + unusedPort() + " ssl;")
                .replace("/etc/nginx/cert/lb/", "/test/");
            Files.writeString(directory.resolve("tls.conf"), config(tls));
            docker("run", "--rm", "-v", directory + ":/test:ro", nginxImage, "nginx", "-t", "-c", "/test/tls.conf");
            System.out.println("T-04 real HTTP: direct IPv4/IPv6 + single/double Nginx, forged/malformed XFF, "
                + "whitelist, production rate key, OperLogEvent; invalid trusted chain rejected before audit; TLS nginx -t passed");
        } finally {
            server.stop();
            RequestContextHolder.resetRequestAttributes();
            ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", null);
            ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", null);
        }
    }

    private void probe(HttpClient client, String url, String expected, boolean doubleProxy, String forged) throws Exception {
        String canonical = NetUtils.parseIpLiteral(expected).getHostAddress();
        for (String whitelist : List.of(expected, "192.0.2.77")) {
            var builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("Test-Allow-Ip", whitelist).header("X-Real-IP", "192.0.2.77")
                .header("Forwarded", "for=192.0.2.77").header("Proxy-Client-IP", "192.0.2.77");
            if (forged != null) {
                builder.header("X-Forwarded-For", forged);
            }
            var response = client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(url + " socket peers=" + socketPeers).isEqualTo(whitelist.equals(expected) ? 200 : 403);
            Map<String, Object> result = JsonUtils.parseMap(response.body());
            assertThat(result.get("client")).isEqualTo(canonical);
            assertThat(result.get("audit")).isEqualTo(canonical);
            assertThat((String) result.get("rateKey")).contains(":" + canonical + ":").doesNotContain("192.0.2.77");
            if (doubleProxy) {
                assertThat(result.get("xff")).isEqualTo(expected + ", 127.0.0.3");
                assertThat(result.get("peer")).isEqualTo("127.0.0.2");
                assertThat(result.get("forwarded")).isEqualTo("");
            }
        }
    }

    private class ProbeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
            try (var stp = mockStatic(StpUtil.class)) {
                stp.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY))
                    .thenReturn(request.getHeader("Test-Allow-Ip"));
                boolean allowed = true;
                try {
                    ReflectionTestUtils.invokeMethod(new SecurityConfig(new SecurityProperties()), "validateClientAccessRules", request);
                } catch (NotPermissionException denied) {
                    allowed = false;
                }
                var point = mock(ProceedingJoinPoint.class);
                var signature = mock(Signature.class);
                when(signature.getName()).thenReturn("probe");
                when(point.getSignature()).thenReturn(signature);
                when(point.getTarget()).thenReturn(this);
                when(point.proceed()).thenReturn(Map.of("ok", true));
                var method = AuditFixture.class.getDeclaredMethod("probe");
                String rateKey = ReflectionTestUtils.invokeMethod(new RateLimiterAspect(), "getCombineKey",
                    method.getAnnotation(RateLimiter.class), point);
                int count = operations.size();
                new LogAspect().doAround(point, method.getAnnotation(Log.class));
                assertThat(operations).hasSize(count + 1);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("client", ServletUtils.getClientIP());
                result.put("peer", request.getRemoteAddr());
                result.put("xff", request.getHeader("X-Forwarded-For"));
                result.put("forwarded", request.getHeader("Forwarded") == null ? "" : request.getHeader("Forwarded"));
                result.put("rateKey", rateKey);
                result.put("audit", operations.getLast().getOperIp());
                response.setStatus(allowed ? 200 : 403);
                response.setContentType("application/json");
                response.getWriter().write(JsonUtils.toJsonString(result));
            } catch (Throwable failure) {
                throw new IOException(failure);
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        }
    }

    private static class AuditFixture {
        @Log(title = "source probe", isSaveRequestData = false, isSaveResponseData = false)
        @RateLimiter(limitType = LimitType.IP)
        public void probe() { }
    }

    private String template(Path repo, String file) throws IOException {
        return Files.readString(repo.resolve("release-artifacts/docker/frontend/nginx/" + file))
            .replace("${LB_SERVER_NAME}", "localhost").replace("${APP_ADMIN_WEB_PREFIX}", "console")
            .replace("${APP_HOME_WEB_PREFIX}", "home").replace("${APP_PREFIX}", "console");
    }

    private String substituteUnusedUpstreams(String source, int backendPort) {
        return source.replace("namewta-nginx-home-web:80", "127.0.0.1:" + backendPort)
            .replace("namewta-monitor-admin:9090", "127.0.0.1:" + backendPort)
            .replace("namewta-snailjob-server:8800", "127.0.0.1:" + backendPort)
            .replace("namewta-snailai-server:8900", "127.0.0.1:" + backendPort)
            .replace("nacos:8848", "127.0.0.1:" + backendPort);
    }

    private String config(String http) {
        return "worker_processes 1;\nerror_log /dev/stderr warn;\npid /tmp/nginx.pid;\nevents { worker_connections 32; }\nhttp {\n"
            + http + "\n}\n";
    }

    private int unusedPort() throws IOException {
        try (var socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            return socket.getLocalPort();
        }
    }

    private void awaitPort(int port) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            try (var socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 100);
                return;
            } catch (IOException ignored) {
                Thread.sleep(50);
            }
        }
        throw new IOException("Test Nginx did not become ready");
    }

    private void docker(String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.addAll(List.of(arguments));
        command(command);
    }

    private void command(List<String> command) throws Exception {
        Path output = Files.createTempFile(directory, "command-", ".log");
        var process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Test command timeout: " + command.getFirst());
        }
        assertThat(process.exitValue()).as(command + "\n" + Files.readString(output)).isZero();
    }
}
