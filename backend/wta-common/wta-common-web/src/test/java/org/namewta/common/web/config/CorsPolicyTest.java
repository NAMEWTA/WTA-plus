package org.namewta.common.web.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.web.config.properties.CorsProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class CorsPolicyTest {
    @Test
    void unknownCredentialedOriginCannotReachLogin() throws Exception {
        var request = new MockHttpServletRequest("POST", "/sso/login");
        request.setServerName("sso.example.test");
        request.setScheme("https");
        request.setServerPort(443);
        request.addHeader("Origin", "https://unknown.example.test");
        var response = new MockHttpServletResponse();
        var reached = new AtomicBoolean();
        new ResourcesConfig().corsFilter(new CorsProperties()).doFilter(request, response,
            (req, res) -> reached.set(true));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(reached).isFalse();
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    void approvedExactOriginPassesPreflightAndCredentialsButLookalikesDoNot() throws Exception {
        var properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://sso.example.test", "http://127.0.0.1:4176"));
        for (String origin : List.of("https://sso.example.test", "http://127.0.0.1:4176",
            "https://sso.example.test.attacker.test", "https://sso.example.test:444", "null")) {
            var request = new MockHttpServletRequest("OPTIONS", "/sso/login");
            request.setServerName("backend.example.test");
            request.addHeader("Origin", origin);
            request.addHeader("Access-Control-Request-Method", "POST");
            request.addHeader("Access-Control-Request-Headers", "Content-Type");
            var response = new MockHttpServletResponse();
            new ResourcesConfig().corsFilter(properties).doFilter(request, response, (req, res) -> { });
            if (properties.getAllowedOrigins().contains(origin)) {
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
                assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
                assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("POST");
            } else {
                assertThat(response.getStatus()).isEqualTo(403);
                assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
            }
        }
    }

    @Test
    void wildcardEchoesHttpOriginAndCredentials() throws Exception {
        var properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("*"));
        var filter = new ResourcesConfig().corsFilter(properties);
        for (String method : List.of("POST", "OPTIONS")) {
            var request = new MockHttpServletRequest(method, "/auth/login");
            request.setServerName("127.0.0.1");
            request.setServerPort(38888);
            request.addHeader("Origin", "http://172.16.105.9:5177");
            if ("OPTIONS".equals(method)) {
                request.addHeader("Access-Control-Request-Method", "POST");
                request.addHeader("Access-Control-Request-Headers", "Content-Type");
            }
            var response = new MockHttpServletResponse();
            var reached = new AtomicBoolean();
            filter.doFilter(request, response, (req, res) -> reached.set(true));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(reached.get()).isEqualTo("POST".equals(method));
            assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://172.16.105.9:5177");
            assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        }
    }

    @Test
    void wildcardStillRejectsNonHttpOrigins() throws Exception {
        var properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("*"));
        var filter = new ResourcesConfig().corsFilter(properties);
        for (String origin : List.of("null", "file:///tmp")) {
            var request = new MockHttpServletRequest("POST", "/auth/login");
            request.addHeader("Origin", origin);
            var response = new MockHttpServletResponse();
            var reached = new AtomicBoolean();
            filter.doFilter(request, response, (req, res) -> reached.set(true));
            assertThat(response.getStatus()).as(origin).isEqualTo(403);
            assertThat(reached).as(origin).isFalse();
            assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
        }
    }

    @Test
    void mixedExactIpAndDomainPatternsPermitMatchingOriginsOnly() throws Exception {
        var properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://admin.example.test", "http://192.168.*:*",
            "https://*.internal.test:8443", "http://10.*:[*]"));
        var filter = new ResourcesConfig().corsFilter(properties);
        for (String origin : List.of("https://admin.example.test", "http://192.168.10.5:5177",
            "https://portal.internal.test:8443", "http://10.20.30.40:8080")) {
            for (String method : List.of("POST", "OPTIONS")) {
                var request = new MockHttpServletRequest(method, "/auth/login");
                request.addHeader("Origin", origin);
                if ("OPTIONS".equals(method)) request.addHeader("Access-Control-Request-Method", "POST");
                var response = new MockHttpServletResponse();
                filter.doFilter(request, response, (req, res) -> { });
                assertThat(response.getStatus()).as(origin).isEqualTo(200);
                assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
                assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
            }
        }
        for (String origin : List.of("http://172.16.0.1:5177", "https://portal.internal.test:8444",
            "https://portal.internal.test.attacker.test:8443", "null")) {
            var request = new MockHttpServletRequest("POST", "/auth/login");
            request.addHeader("Origin", origin);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> { });
            assertThat(response.getStatus()).as(origin).isEqualTo(403);
        }
    }

    @Test
    void unrestrictedWildcardCanBeCombinedWithExactAndPartialOrigins() {
        var properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("*", "http://127.0.0.1:4176", "https://*.internal.test"));
        assertThat(properties.allowsAnyHttpOrigin()).isTrue();
        assertThat(properties.validatedOrigins()).containsExactly("http://127.0.0.1:4176");
        assertThat(properties.validatedOriginPatterns()).containsExactly("http://*", "https://*", "https://*.internal.test");
    }

    @Test
    void emptyConfigurationAllowsNoCrossOriginAndNoOriginRequestPasses() throws Exception {
        var properties = new CorsProperties();
        properties.setAllowedOrigins(List.of(""));
        var filter = new ResourcesConfig().corsFilter(properties);
        assertThat(properties.validatedOrigins()).isEmpty();
        var request = new MockHttpServletRequest("POST", "/sso/login");
        var response = new MockHttpServletResponse();
        var reached = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> reached.set(true));
        assertThat(reached).isTrue();
        assertThat(response.getStatus()).isNotEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void exactOriginWithoutCredentialsDoesNotAdvertiseCredentialAccess() throws Exception {
        var properties = new CorsProperties();
        properties.setAllowCredentials(false);
        properties.setAllowedOrigins(List.of("https://admin.example.test"));
        var request = new MockHttpServletRequest("GET", "/api/resource");
        request.setServerName("backend.example.test");
        request.addHeader("Origin", "https://admin.example.test");
        var response = new MockHttpServletResponse();
        new ResourcesConfig().corsFilter(properties).doFilter(request, response, (req, res) -> { });
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://admin.example.test");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    void invalidOriginConfigurationIsRejected() {
        for (String invalid : List.of("null", "file:///tmp", "https://user@example.test",
            "https://example.test/", "https://example.test/path", "https://example.test?x=1",
            "https://example.test#fragment", "https://example.test:65536", "https://example.test:",
            "https://*.example.test/path", "https://*.example.test:65536", "*://example.test",
            "https://user@*.example.test", "https://*.example.test:[8080,8443]")) {
            var properties = new CorsProperties();
            properties.setAllowedOrigins(List.of(invalid));
            assertThatThrownBy(() -> new ResourcesConfig().corsFilter(properties)).as(invalid)
                .isInstanceOf(IllegalArgumentException.class);
        }
        var mixed = new CorsProperties();
        mixed.setAllowedOrigins(List.of("*", "https://*.example.test/path"));
        assertThatThrownBy(() -> new ResourcesConfig().corsFilter(mixed))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
