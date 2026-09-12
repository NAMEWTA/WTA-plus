package org.namewta.test.openapi.gateway;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.common.openapi.annotation.OpenApi;
import org.namewta.common.openapi.config.properties.OpenApiProperties;
import org.namewta.common.openapi.gateway.OpenApiGatewayFilter;
import org.namewta.common.openapi.nonce.OpenApiNonceStore;
import org.namewta.common.openapi.protocol.OpenApiCanonicalizer;
import org.namewta.common.openapi.protocol.OpenApiHeaders;
import org.namewta.common.openapi.protocol.OpenApiRequest;
import org.namewta.common.openapi.protocol.OpenApiSigner;
import org.namewta.common.openapi.ratelimit.OpenApiRateLimiter;
import org.namewta.common.openapi.registry.OpenApiAuthorizationMatcher;
import org.namewta.common.openapi.registry.OpenApiOperationRegistry;
import org.namewta.common.openapi.registry.SpringDocOperationSchemaResolver;
import org.namewta.common.openapi.session.OpenApiMachineSessionBridge;
import org.namewta.common.openapi.session.OpenApiMachineSessionOperations;
import org.namewta.common.openapi.session.VerifiedOpenApiIdentity;
import org.namewta.common.openapi.spi.OpenApiCallEvent;
import org.namewta.common.openapi.spi.OpenApiCallEventPublisher;
import org.namewta.common.openapi.spi.OpenApiCredential;
import org.namewta.common.security.config.SecurityConfig;
import org.namewta.system.api.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class OpenApiGatewayE2ETest {

    private static final Instant NOW = Instant.parse("2026-08-31T12:00:00Z");
    private static final String APP_KEY = "YXBwLWtleS13aXRoLTEyOC1iaXRzLW1pbmltdW0";
    private static final String APP_SECRET = "c2VjcmV0LXdpdGgtMjU2LWJpdHMtbWluaW11bS1rZXktbWF0ZXJpYWw";
    private static final String BODY = "{\"value\":\"signed-body\"}";

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private TestController controller;
    private final RecordingNonceStore nonceStore = new RecordingNonceStore();
    private final RecordingRateLimiter rateLimiter = new RecordingRateLimiter();
    private final RecordingSessionOperations sessions = new RecordingSessionOperations();
    private final List<OpenApiCallEvent> events = new ArrayList<>();
    private final OpenApiSigner signer = new OpenApiSigner(new OpenApiCanonicalizer());
    private boolean grantPermission = true;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestWebConfiguration.class);
        context.refresh();
        controller = context.getBean(TestController.class);
        RequestMappingHandlerMapping mappings = context.getBean(RequestMappingHandlerMapping.class);
        OpenApiOperationRegistry registry = new OpenApiOperationRegistry(
            mappings, new SpringDocOperationSchemaResolver());
        registry.afterPropertiesSet();
        OpenApiProperties properties = new OpenApiProperties();
        OpenApiMachineSessionBridge bridge = new OpenApiMachineSessionBridge(
            this::authorizedUser, sessions, Duration.ofHours(8));
        OpenApiCallEventPublisher publisher = event -> {
            events.add(event);
            throw new IllegalStateException("metering-down");
        };
        OpenApiGatewayFilter filter = new OpenApiGatewayFilter(
            mappings, registry,
            appKey -> new OpenApiCredential(7L, 9L, APP_KEY, APP_SECRET, NOW.plusSeconds(3600)),
            signer, nonceStore, rateLimiter, bridge, new OpenApiAuthorizationMatcher(), publisher,
            properties, Clock.fixed(NOW, ZoneOffset.UTC), sessions::currentUser);
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
            .addFilters(filter)
            .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void validSignatureInvokesOnlyPublishedHandlerWithExactBodyAndCleansMachineContext() throws Exception {
        mvc.perform(signed(post("/gateway/echo").contentType(MediaType.APPLICATION_JSON).content(BODY),
                "POST", "/gateway/echo", "", BODY, "nonce-valid-0001"))
            .andExpect(status().isOk())
            .andExpect(content().json(BODY));

        assertThat(controller.echoCalls).isEqualTo(1);
        assertThat(sessions.currentUser()).isNull();
        assertThat(rateLimiter.scopes).hasSize(2);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.credentialId()).isEqualTo(7L);
            assertThat(event.ownerUserId()).isEqualTo(9L);
            assertThat(event.path()).isEqualTo("/gateway/echo");
            assertThat(event.status()).isEqualTo(200);
        });
    }

    @Test
    void unsignedBrowserRequestBypassesMachineChainAndUnpublishedSignedRequestNeverExecutes() throws Exception {
        mvc.perform(get("/gateway/browser"))
            .andExpect(status().isOk())
            .andExpect(content().string("browser"));
        mvc.perform(signed(get("/gateway/browser"), "GET", "/gateway/browser", "", "", "nonce-hidden-0001"))
            .andExpect(status().isForbidden())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("OPENAPI_FORBIDDEN")));

        assertThat(controller.browserCalls).isEqualTo(1);
        assertThat(nonceStore.seen).isEmpty();
    }

    @Test
    void malformedExpiredTamperedReplayedAndDualAuthRequestsShareOneAuthenticationFailure() throws Exception {
        MockHttpServletRequestBuilder partial = post("/gateway/echo").header(OpenApiHeaders.APP_KEY, APP_KEY);
        mvc.perform(partial).andExpect(status().isUnauthorized())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("OPENAPI_AUTHENTICATION_FAILED")));

        mvc.perform(signed(post("/gateway/echo").content(BODY), "POST", "/gateway/echo", "", BODY,
                "nonce-dual-000001").header("Authorization", "browser-token"))
            .andExpect(status().isUnauthorized());

        mvc.perform(signed(post("/gateway/echo").content(BODY).cookie(
                new jakarta.servlet.http.Cookie("Authorization", "browser-token")),
                "POST", "/gateway/echo", "", BODY, "nonce-cookie-0001"))
            .andExpect(status().isUnauthorized());

        mvc.perform(signed(post("/gateway/echo?Token=browser-token").content(BODY),
                "POST", "/gateway/echo", "Token=browser-token", BODY, "nonce-query-00001"))
            .andExpect(status().isUnauthorized());

        mvc.perform(signedAt(post("/gateway/echo").content(BODY), "POST", "/gateway/echo", BODY,
                "nonce-expired-001", NOW.minusSeconds(61)))
            .andExpect(status().isUnauthorized());

        mvc.perform(signed(post("/gateway/echo").content(BODY + "x"), "POST", "/gateway/echo", "", BODY,
                "nonce-tamper-0001"))
            .andExpect(status().isUnauthorized());

        MockHttpServletRequestBuilder replay = signed(post("/gateway/echo").content(BODY), "POST",
            "/gateway/echo", "", BODY, "nonce-replay-0001");
        mvc.perform(replay).andExpect(status().isOk());
        mvc.perform(signed(post("/gateway/echo").content(BODY), "POST", "/gateway/echo", "", BODY,
                "nonce-replay-0001"))
            .andExpect(status().isUnauthorized());

        assertThat(sessions.currentUser()).isNull();
    }

    @Test
    void eitherRateLimitRejectsAndStateStoreFailureIsUnavailable() throws Exception {
        rateLimiter.rejectedCall = 2;
        mvc.perform(signed(post("/gateway/echo").content(BODY), "POST", "/gateway/echo", "", BODY,
                "nonce-rate-000001"))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("OPENAPI_RATE_LIMITED")));
        assertThat(rateLimiter.scopes).hasSize(2);

        nonceStore.failure = new IllegalStateException("redis-down");
        mvc.perform(signed(post("/gateway/echo").content(BODY), "POST", "/gateway/echo", "", BODY,
                "nonce-redis-00001"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("OPENAPI_UNAVAILABLE")));
        assertThat(sessions.currentUser()).isNull();
    }

    @Test
    void deniedAuthorizationCleansSessionAndVerifiedChannelPredicateCannotBypassClientChecks() throws Exception {
        grantPermission = false;
        mvc.perform(signed(post("/gateway/echo").content(BODY), "POST", "/gateway/echo", "", BODY,
                "nonce-denied-0001"))
            .andExpect(status().isForbidden())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("OPENAPI_FORBIDDEN")));
        assertThat(controller.echoCalls).isZero();
        assertThat(sessions.currentUser()).isNull();

        var method = SecurityConfig.class.getDeclaredMethod(
            "isVerifiedOpenApiRequest", jakarta.servlet.http.HttpServletRequest.class, LoginUser.class);
        method.setAccessible(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUser machine = authorizedUser(9L);
        request.setAttribute(OpenApiGatewayFilter.VERIFIED_REQUEST_ATTRIBUTE, Boolean.TRUE);
        assertThat(method.invoke(null, request, machine)).isEqualTo(true);

        machine.setClientPk(3L);
        assertThat(method.invoke(null, request, machine)).isEqualTo(false);
        machine.setClientPk(null);
        machine.setUserType("web");
        assertThat(method.invoke(null, request, machine)).isEqualTo(false);
        request.removeAttribute(OpenApiGatewayFilter.VERIFIED_REQUEST_ATTRIBUTE);
        machine.setUserType("openapi");
        assertThat(method.invoke(null, request, machine)).isEqualTo(false);
    }

    @Test
    void downstreamFailureRemainsAControllerFailureAndStillCleansContext() {
        assertThatThrownBy(() -> mvc.perform(signed(get("/gateway/fail"), "GET", "/gateway/fail", "", "",
            "nonce-failure-001")))
            .hasRootCauseInstanceOf(MarkerException.class);

        assertThat(sessions.currentUser()).isNull();
        assertThat(events).singleElement().extracting(OpenApiCallEvent::status).isEqualTo(500);
    }

    @Test
    void multiPathHandlerUsesTheActuallyMatchedRegistryOperation() throws Exception {
        mvc.perform(signed(get("/gateway/multi/41"), "GET", "/gateway/multi/41", "", "",
                "nonce-multi-00001"))
            .andExpect(status().isOk())
            .andExpect(content().string("41"));
        String firstInterface = events.getLast().interfaceId();

        mvc.perform(signed(get("/gateway/alias/42"), "GET", "/gateway/alias/42", "", "",
                "nonce-alias-00001"))
            .andExpect(status().isOk())
            .andExpect(content().string("42"));

        assertThat(events.getLast().interfaceId()).isNotEqualTo(firstInterface);
        assertThat(sessions.currentUser()).isNull();
    }

    private MockHttpServletRequestBuilder signed(MockHttpServletRequestBuilder builder, String method,
                                                   String path, String query, String body, String nonce) {
        return signedAt(builder, method, path, body, nonce, NOW, query);
    }

    private MockHttpServletRequestBuilder signedAt(MockHttpServletRequestBuilder builder, String method,
                                                     String path, String body, String nonce, Instant timestamp) {
        return signedAt(builder, method, path, body, nonce, timestamp, "");
    }

    private MockHttpServletRequestBuilder signedAt(MockHttpServletRequestBuilder builder, String method,
                                                     String path, String body, String nonce, Instant timestamp,
                                                     String query) {
        String seconds = Long.toString(timestamp.getEpochSecond());
        String wireNonce = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(nonce.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        OpenApiRequest request = new OpenApiRequest(APP_KEY, seconds, wireNonce, method, path, query,
            body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return builder
            .header(OpenApiHeaders.VERSION, "v1")
            .header(OpenApiHeaders.APP_KEY, APP_KEY)
            .header(OpenApiHeaders.TIMESTAMP, seconds)
            .header(OpenApiHeaders.NONCE, wireNonce)
            .header(OpenApiHeaders.SIGNATURE, signer.sign(request, APP_SECRET));
    }

    private LoginUser authorizedUser(Long userId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUserType("openapi");
        user.setMenuPermission(grantPermission ? Set.of("orders:read") : Set.of());
        user.setRolePermission(Set.of());
        return user;
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfiguration {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    @RequestMapping("/gateway")
    static class TestController {
        int echoCalls;
        int browserCalls;

        @OpenApi("Echo")
        @SaCheckPermission("orders:read")
        @PostMapping("/echo")
        String echo(@RequestBody String body) {
            echoCalls++;
            return body;
        }

        @GetMapping("/browser")
        String browser() {
            browserCalls++;
            return "browser";
        }

        @OpenApi("Failure")
        @SaCheckPermission("orders:read")
        @GetMapping("/fail")
        String fail() {
            throw new MarkerException();
        }

        @OpenApi("Multi path")
        @SaCheckPermission("orders:read")
        @GetMapping({"/multi/{id}", "/alias/{id}"})
        String multi(@PathVariable("id") String id) {
            return id;
        }
    }

    static final class RecordingNonceStore implements OpenApiNonceStore {
        final Set<String> seen = new HashSet<>();
        RuntimeException failure;

        @Override
        public boolean register(String appKey, String nonce, Duration ttl) {
            if (failure != null) {
                throw failure;
            }
            return seen.add(appKey + ':' + nonce);
        }
    }

    static final class RecordingRateLimiter implements OpenApiRateLimiter {
        final List<String> scopes = new ArrayList<>();
        int rejectedCall = -1;

        @Override
        public boolean acquire(String scope, int limit, Duration interval) {
            scopes.add(scope);
            return scopes.size() != rejectedCall;
        }
    }

    static final class RecordingSessionOperations implements OpenApiMachineSessionOperations {
        private final ThreadLocal<LoginUser> current = new ThreadLocal<>();
        private LoginUser stored;

        LoginUser currentUser() {
            return current.get();
        }

        @Override
        public LoginUser find(VerifiedOpenApiIdentity identity) {
            return stored;
        }

        @Override
        public void create(VerifiedOpenApiIdentity identity, LoginUser loginUser, Duration ttl) {
            stored = loginUser;
        }

        @Override
        public <T> T inRequestScope(VerifiedOpenApiIdentity identity, Supplier<T> callback) {
            current.set(stored);
            try {
                return callback.get();
            } finally {
                current.remove();
            }
        }

        @Override
        public <T> T withUserLock(Long userId, Supplier<T> callback) {
            return callback.get();
        }

        @Override
        public int invalidateByUserId(Long userId) {
            stored = null;
            return 1;
        }
    }

    static final class MarkerException extends RuntimeException {
    }
}
