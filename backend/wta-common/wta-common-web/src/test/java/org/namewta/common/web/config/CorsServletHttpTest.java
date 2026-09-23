package org.namewta.common.web.config;

import jakarta.servlet.DispatcherType;
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
import org.namewta.common.web.config.properties.CorsProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** 通过真实 Servlet 容器验证全局 CorsFilter 的来源边界。 */
@Tag("dev")
class CorsServletHttpTest {

    @Test
    void exactOriginsPassAndHostileOriginsNeverReceiveCorsPermission() throws Exception {
        var properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://admin.example.test", "http://127.0.0.1:5177"));
        try (var server = new TestServer(properties)) {
            var noOrigin = server.request("GET", null, false);
            assertThat(noOrigin.statusCode()).isEqualTo(204);
            assertThat(noOrigin.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();

            var sameOrigin = server.request("POST", server.origin(), false);
            assertThat(sameOrigin.statusCode()).isEqualTo(204);

            for (String trusted : List.of("https://admin.example.test", "http://127.0.0.1:5177")) {
                var actual = server.request("POST", trusted, false);
                assertThat(actual.statusCode()).isEqualTo(204);
                assertThat(actual.headers().firstValue("Access-Control-Allow-Origin")).hasValue(trusted);
                assertThat(actual.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");

                int beforePreflight = server.servedRequests();
                var preflight = server.request("OPTIONS", trusted, true);
                assertThat(preflight.statusCode()).isEqualTo(200);
                assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin")).hasValue(trusted);
                assertThat(preflight.headers().firstValue("Access-Control-Allow-Methods")).hasValueSatisfying(
                    methods -> assertThat(methods).contains("POST"));
                assertThat(preflight.headers().firstValue("Access-Control-Allow-Headers")).hasValueSatisfying(
                    headers -> assertThat(headers).containsIgnoringCase("content-type"));
                assertThat(server.servedRequests()).isEqualTo(beforePreflight);
            }

            for (String hostile : List.of("https://admin.example.test.attacker.test",
                "https://admin.example.test:444", "null", "http://127.0.0.1:5178")) {
                int before = server.servedRequests();
                for (String method : List.of("POST", "OPTIONS")) {
                    var rejected = server.request(method, hostile, "OPTIONS".equals(method));
                    assertThat(rejected.statusCode()).isEqualTo(403);
                    assertThat(rejected.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
                    assertThat(rejected.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
                }
                assertThat(server.servedRequests()).isEqualTo(before);
            }
        }
    }

    @Test
    void defaultPolicyKeepsSameOriginWorkingButRejectsCrossOrigin() throws Exception {
        try (var server = new TestServer(new CorsProperties())) {
            assertThat(server.request("GET", null, false).statusCode()).isEqualTo(204);
            assertThat(server.request("POST", server.origin(), false).statusCode()).isEqualTo(204);
            var rejected = server.request("POST", "https://unknown.example.test", false);
            assertThat(rejected.statusCode()).isEqualTo(403);
            assertThat(rejected.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        }
    }

    @Test
    void explicitOriginWithoutCredentialsNeverAdvertisesCredentialAccess() throws Exception {
        var properties = new CorsProperties();
        properties.setAllowCredentials(false);
        properties.setAllowedOrigins(List.of("https://admin.example.test"));
        try (var server = new TestServer(properties)) {
            var response = server.request("POST", "https://admin.example.test", false);
            assertThat(response.statusCode()).isEqualTo(204);
            assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .hasValue("https://admin.example.test");
            assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
        }
    }

    private static final class TestServer implements AutoCloseable {
        private final Server server = new Server();
        private final AtomicInteger served = new AtomicInteger();
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        private final URI base;

        private TestServer(CorsProperties properties) throws Exception {
            var connector = new ServerConnector(server);
            connector.setHost("127.0.0.1");
            connector.setPort(0);
            server.addConnector(connector);
            var handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.addFilter(new FilterHolder(new ResourcesConfig().corsFilter(properties)), "/*",
                EnumSet.of(DispatcherType.REQUEST));
            handler.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void service(HttpServletRequest request, HttpServletResponse response) {
                    served.incrementAndGet();
                    response.setStatus(204);
                }
            }), "/*");
            server.setHandler(handler);
            server.start();
            base = URI.create("http://127.0.0.1:" + connector.getLocalPort());
        }

        private String origin() {
            return base.toString();
        }

        private int servedRequests() {
            return served.get();
        }

        private HttpResponse<Void> request(String method, String origin, boolean preflight) throws Exception {
            var builder = HttpRequest.newBuilder(base.resolve("/sso/login"))
                .timeout(Duration.ofSeconds(5));
            if (origin != null) {
                builder.header("Origin", origin);
            }
            if (preflight) {
                builder.header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "Content-Type");
            }
            builder.method(method, "POST".equals(method)
                ? HttpRequest.BodyPublishers.ofString("{}") : HttpRequest.BodyPublishers.noBody());
            return client.send(builder.build(), HttpResponse.BodyHandlers.discarding());
        }

        @Override
        public void close() throws Exception {
            server.stop();
            server.join();
            client.close();
        }
    }
}
