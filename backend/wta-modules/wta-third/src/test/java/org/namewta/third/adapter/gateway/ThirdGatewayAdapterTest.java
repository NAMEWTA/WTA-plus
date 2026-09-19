package org.namewta.third.adapter.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.api.ThirdPartyRequest;
import org.namewta.third.api.ThirdPartyResponse;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.port.ThirdConfigSnapshot;
import org.namewta.third.port.ThirdConfigSnapshotPort;
import org.namewta.third.port.ThirdCredentialCryptoPort;
import org.namewta.third.port.ThirdCredentialStore;
import org.namewta.third.port.ThirdInvocationRecorderPort;
import org.namewta.third.port.ThirdOutboundAttempt;
import org.namewta.third.port.ThirdResiliencePort;
import org.namewta.third.spi.ThirdProviderAdapterRegistry;
import org.namewta.third.support.ThirdLimitLease;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
class ThirdGatewayAdapterTest {
    private final JsonMapper jsonMapper = new JsonMapper();
    private HttpServer server;
    private AtomicInteger requests;
    private AtomicInteger redirectTargetRequests;
    private ThirdConfigSnapshot snapshot;
    private ThirdGatewayAdapter gateway;
    private List<ThirdOutboundAttempt> outboundAttempts;

    @BeforeEach
    void setUp() throws IOException {
        requests = new AtomicInteger();
        redirectTargetRequests = new AtomicInteger();
        outboundAttempts = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/companies", this::handleCompanyRequest);
        server.createContext("/redirect", this::handleRedirect);
        server.createContext("/redirect-target", this::handleRedirectTarget);
        server.start();

        ThirdProvider provider = new ThirdProvider();
        provider.setProviderId(1L);
        provider.setProviderCode("qichacha");
        provider.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        provider.setStatus("0");
        provider.setTimeoutConnectMs(1_000);
        provider.setTimeoutReadMs(1_000);
        provider.setSharedHeadersJson("{\"X-Shared\":\"shared\"}");

        ThirdEndpoint endpoint = new ThirdEndpoint();
        endpoint.setEndpointId(2L);
        endpoint.setProviderId(1L);
        endpoint.setProviderCode("qichacha");
        endpoint.setEndpointCode("company");
        endpoint.setHttpMethod("POST");
        endpoint.setRelativePath("/companies/{id}");
        endpoint.setRequestMode("JSON");
        endpoint.setResponseMode("JSON");
        endpoint.setPathSchemaJson("{\"allowed\":[\"id\"]}");
        endpoint.setQuerySchemaJson("{\"allowed\":[\"trace\"]}");
        endpoint.setHeaderSchemaJson("{\"allowed\":[\"X-Caller\"]}");
        endpoint.setBodySchemaJson("{\"allowed\":[\"name\"]}");
        endpoint.setResponseSchemaJson("{}");
        endpoint.setOverrideJson("{}");
        endpoint.setSensitiveFieldsJson("[]");
        endpoint.setStatus("0");
        endpoint.setIdempotent(false);
        snapshot = new ThirdConfigSnapshot(provider, endpoint);

        ThirdConfigSnapshotPort config = new ThirdConfigSnapshotPort() {
            @Override
            public ThirdConfigSnapshot get(String providerCode, String endpointCode) {
                return snapshot;
            }

            @Override
            public void evict(String providerCode, String endpointCode) {
            }
        };
        ThirdResiliencePort resilience = new ThirdResiliencePort() {
            @Override
            public ThirdLimitLease acquire(ThirdProvider provider, ThirdEndpoint endpoint) {
                return () -> {
                };
            }

            @Override
            public void refresh(ThirdProvider provider, ThirdEndpoint endpoint) { }

            @Override
            public int maxAttempts(ThirdEndpoint endpoint) {
                return 1;
            }
        };
        gateway = createGateway(resilience, (request, response, durationMs, attempts) -> {
        });
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsConstrainedJsonRequestToLoopbackServer() {
        ThirdPartyRequest request = new ThirdPartyRequest("qichacha", "company", Map.of("id", "acme/1"),
            Map.of("trace", "t-1"), Map.of("X-Caller", "caller"), jsonMapper.readTree("{\"name\":\"Acme\"}"));

        ThirdPartyResponse<Object> response = gateway.execute(request);

        assertTrue(response.isSuccess());
        assertEquals(200, response.httpStatus());
        assertEquals(1, requests.get());
        assertEquals(2, outboundAttempts.size());
        assertEquals(false, outboundAttempts.getFirst().completed());
        assertEquals(true, outboundAttempts.getLast().completed());
        JsonNode body = (JsonNode) response.body();
        assertNotNull(body);
        assertEquals("ok", body.get("result").asText());
    }

    @Test
    void providerDisabledRejectsBeforeAnyHttpRequest() {
        snapshot.getProvider().setStatus("1");

        ThirdPartyResponse<Object> response = gateway.execute(ThirdPartyRequest.of("qichacha", "company"));

        assertEquals(ThirdPartyFailureCategory.PROVIDER_DISABLED, response.category());
        assertEquals(0, requests.get());
    }

    @Test
    void rateLimitRejectionDoesNotReachHttpServer() {
        ThirdResiliencePort rejecting = new ThirdResiliencePort() {
            @Override
            public ThirdLimitLease acquire(ThirdProvider provider, ThirdEndpoint endpoint) {
                throw new org.namewta.third.support.ThirdRejectedException(ThirdPartyFailureCategory.RATE_LIMITED,
                    "rate limited");
            }

            @Override
            public void refresh(ThirdProvider provider, ThirdEndpoint endpoint) { }

            @Override
            public int maxAttempts(ThirdEndpoint endpoint) {
                return 1;
            }
        };
        gateway = createGateway(rejecting, (request, response, durationMs, attempts) -> {
        });

        ThirdPartyResponse<Object> response = gateway.execute(ThirdPartyRequest.of("qichacha", "company"));

        assertEquals(ThirdPartyFailureCategory.RATE_LIMITED, response.category());
        assertEquals(0, requests.get());
    }

    @Test
    void refusesRedirectResponseWithoutFollowingIt() {
        snapshot.getEndpoint().setRelativePath("/redirect");
        snapshot.getEndpoint().setHttpMethod("GET");

        ThirdPartyResponse<Object> response = gateway.execute(ThirdPartyRequest.of("qichacha", "company"));

        assertEquals(ThirdPartyFailureCategory.HTTP, response.category());
        assertEquals(302, response.httpStatus());
        assertEquals(0, redirectTargetRequests.get());
    }

    @Test
    void rejectsEndpointAdapterOwnedByAnotherProvider() {
        snapshot.getEndpoint().setAdapterCode("other-provider-adapter");

        ThirdPartyResponse<Object> response = gateway.execute(ThirdPartyRequest.of("qichacha", "company"));

        assertEquals(ThirdPartyFailureCategory.CONFIG_UNAVAILABLE, response.category());
        assertEquals(0, requests.get());
    }

    private ThirdGatewayAdapter createGateway(ThirdResiliencePort resilience, ThirdInvocationRecorderPort recorder) {
        ThirdConfigSnapshotPort config = new ThirdConfigSnapshotPort() {
            @Override
            public ThirdConfigSnapshot get(String providerCode, String endpointCode) {
                return snapshot;
            }

            @Override
            public void evict(String providerCode, String endpointCode) {
            }
        };
        ThirdInvocationRecorderPort effectiveRecorder = new ThirdInvocationRecorderPort() {
            @Override
            public void record(ThirdPartyRequest request, ThirdPartyResponse<?> response, long durationMs, int attempts) {
                recorder.record(request, response, durationMs, attempts);
            }

            @Override
            public void recordAttempt(ThirdOutboundAttempt attempt) {
                outboundAttempts.add(attempt);
                recorder.recordAttempt(attempt);
            }
        };
        return new ThirdGatewayAdapter(config, new ThirdProviderAdapterRegistry(List.of()), resilience,
            effectiveRecorder, (providerId, endpointId) -> List.of(), new NoopCrypto(),
            new org.namewta.third.http.ThirdHttpClientFactory());
    }

    private void handleCompanyRequest(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        assertEquals("POST", exchange.getRequestMethod());
        assertEquals("shared", exchange.getRequestHeaders().getFirst("X-Shared"));
        assertEquals("caller", exchange.getRequestHeaders().getFirst("X-Caller"));
        assertEquals("t-1", exchange.getRequestURI().getQuery().substring("trace=".length()));
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(requestBody.contains("\"name\":\"Acme\""));
        byte[] response = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private void handleRedirect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "http://example.invalid/blocked");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void handleRedirectTarget(HttpExchange exchange) throws IOException {
        redirectTargetRequests.incrementAndGet();
        byte[] response = "{\"result\":\"unexpected\"}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private static final class NoopCrypto implements ThirdCredentialCryptoPort {
        @Override
        public EncryptedSecret encrypt(String scopeType, String credentialType, String json) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String decrypt(org.namewta.third.domain.ThirdCredential credential) {
            throw new UnsupportedOperationException();
        }
    }
}
