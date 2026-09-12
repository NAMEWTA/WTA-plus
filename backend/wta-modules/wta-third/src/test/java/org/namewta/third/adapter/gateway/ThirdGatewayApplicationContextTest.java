package org.namewta.third.adapter.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.api.ThirdPartyGateway;
import org.namewta.third.api.ThirdPartyRequest;
import org.namewta.third.api.ThirdPartyResponse;
import org.namewta.third.domain.ThirdCredential;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.http.ThirdHttpClientFactory;
import org.namewta.third.port.ThirdConfigSnapshot;
import org.namewta.third.port.ThirdConfigSnapshotPort;
import org.namewta.third.port.ThirdCredentialCryptoPort;
import org.namewta.third.port.ThirdCredentialStore;
import org.namewta.third.port.ThirdEndpointConfigStore;
import org.namewta.third.port.ThirdInvocationRecorderPort;
import org.namewta.third.port.ThirdOutboundAttempt;
import org.namewta.third.port.ThirdResiliencePort;
import org.namewta.third.spi.ThirdAdapterResponse;
import org.namewta.third.spi.ThirdProviderAdapter;
import org.namewta.third.spi.ThirdProviderAdapterRegistry;
import org.namewta.third.spi.ThirdProviderAdapterStartupValidator;
import org.namewta.third.support.ThirdLimitLease;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("local")
class ThirdGatewayApplicationContextTest {

    private final Map<String, AtomicInteger> requests = new ConcurrentHashMap<>();
    private final RecordingInvocationRecorder recorder = new RecordingInvocationRecorder();
    private AnnotationConfigApplicationContext context;
    private ExecutorService httpExecutor;
    private HttpServer server;
    private ThirdConfigSnapshot snapshot;
    private ThirdPartyGateway gateway;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpExecutor = Executors.newCachedThreadPool();
        server.setExecutor(httpExecutor);
        server.createContext("/json", exchange -> respond(exchange, 200, "application/json", "{\"result\":\"ok\"}"));
        server.createContext("/list", exchange -> respond(exchange, 200, "application/json",
            "[{\"result\":\"one\"},{\"result\":\"two\"}]"));
        server.createContext("/text", exchange -> respond(exchange, 200, "text/plain", "plain-text"));
        server.createContext("/bytes", exchange -> respond(exchange, 200, "application/octet-stream", new byte[]{1, 2, 3}));
        server.createContext("/business", exchange -> respond(exchange, 200, "application/json",
            "{\"code\":\"DENIED\",\"message\":\"provider denied\"}"));
        server.createContext("/status", exchange -> respond(exchange, 503, "application/json", "{\"error\":\"down\"}"));
        server.createContext("/redirect", exchange -> {
            counted(exchange);
            exchange.getResponseHeaders().set("Location", "http://127.0.0.1:" + server.getAddress().getPort() + "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> respond(exchange, 200, "application/json", "{\"result\":\"wrong\"}"));
        server.createContext("/timeout", exchange -> delayed(exchange, 300, "{\"result\":\"late\"}"));
        server.createContext("/flaky", exchange -> {
            if (counted(exchange) == 1) {
                sleep(300);
                write(exchange, 200, "application/json", "{\"result\":\"late\"}".getBytes(StandardCharsets.UTF_8));
            } else {
                write(exchange, 200, "application/json", "{\"result\":\"retried\"}".getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();

        ThirdProvider provider = new ThirdProvider();
        provider.setProviderId(1L);
        provider.setProviderCode("qichacha");
        provider.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        provider.setStatus("0");
        provider.setTimeoutConnectMs(500);
        provider.setTimeoutReadMs(100);
        provider.setSharedHeadersJson("{}");

        ThirdEndpoint endpoint = endpoint("/json", "JSON");
        snapshot = new ThirdConfigSnapshot(provider, endpoint);

        context = new AnnotationConfigApplicationContext();
        context.registerBean(ThirdConfigSnapshotPort.class, () -> new SnapshotPort(snapshot));
        context.registerBean(ThirdResiliencePort.class, () -> new TestResilience());
        context.registerBean(ThirdInvocationRecorderPort.class, () -> recorder);
        context.registerBean(ThirdCredentialStore.class, () -> (providerId, endpointId) -> List.of());
        context.registerBean(ThirdCredentialCryptoPort.class, NoopCrypto::new);
        context.registerBean("qichachaAdapter", ThirdProviderAdapter.class, BusinessAdapter::new);
        context.registerBean(ThirdProviderAdapterRegistry.class);
        context.registerBean(ThirdHttpClientFactory.class);
        context.registerBean(ThirdGatewayAdapter.class);
        context.refresh();
        gateway = context.getBean(ThirdPartyGateway.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
        if (server != null) server.stop(0);
        if (httpExecutor != null) httpExecutor.shutdownNow();
    }

    @Test
    void contextBeanSupportsTypedJsonParameterizedTextBytesAndProviderMapping() {
        ThirdPartyResponse<Payload> json = gateway.execute(request(), Payload.class);
        assertThat(json.isSuccess()).isTrue();
        assertThat(json.body()).isEqualTo(new Payload("ok"));

        configure("/list", "JSON", false, 0);
        ThirdPartyResponse<List<Payload>> list = gateway.execute(request(), new ParameterizedTypeReference<>() { });
        assertThat(list.body()).containsExactly(new Payload("one"), new Payload("two"));

        configure("/text", "TEXT", false, 0);
        assertThat(gateway.execute(request(), String.class).body()).isEqualTo("plain-text");

        configure("/bytes", "BYTES", false, 0);
        assertThat(gateway.execute(request(), byte[].class).body()).containsExactly(1, 2, 3);

        configure("/business", "JSON", false, 0);
        ThirdPartyResponse<Object> business = gateway.execute(request());
        assertThat(business.category()).isEqualTo(ThirdPartyFailureCategory.PROVIDER);
        assertThat(business.providerMessage()).isEqualTo("provider denied");
        assertThat(business.body()).isNull();
    }

    @Test
    void contextBeanClassifiesStatusRedirectSecurityTimeoutAndBoundedRetry() {
        configure("/status", "JSON", true, 3);
        ThirdPartyResponse<Object> status = gateway.execute(request());
        assertThat(status.category()).isEqualTo(ThirdPartyFailureCategory.HTTP);
        assertThat(status.httpStatus()).isEqualTo(503);
        assertThat(count("/status")).isEqualTo(1);

        configure("/redirect", "JSON", false, 0);
        ThirdPartyResponse<Object> redirect = gateway.execute(request());
        assertThat(redirect.category()).isEqualTo(ThirdPartyFailureCategory.HTTP);
        assertThat(redirect.httpStatus()).isEqualTo(302);
        assertThat(count("/target")).isZero();

        int sentBeforeSecurityRejection = totalRequests();
        configure("//example.invalid/escape", "JSON", false, 0);
        ThirdPartyResponse<Object> rejected = gateway.execute(request());
        assertThat(rejected.category()).isEqualTo(ThirdPartyFailureCategory.CONFIG_UNAVAILABLE);
        assertThat(totalRequests()).isEqualTo(sentBeforeSecurityRejection);

        configure("/timeout", "JSON", false, 3);
        ThirdPartyResponse<Object> timeout = gateway.execute(request());
        assertThat(timeout.category()).isEqualTo(ThirdPartyFailureCategory.TIMEOUT);
        assertThat(count("/timeout")).isEqualTo(1);

        recorder.clear();
        configure("/flaky", "JSON", true, 1);
        ThirdPartyResponse<Payload> retried = gateway.execute(request(), Payload.class);
        assertThat(retried.isSuccess()).isTrue();
        assertThat(retried.body()).isEqualTo(new Payload("retried"));
        assertThat(count("/flaky")).isEqualTo(2);
        assertThat(recorder.attempts).hasSize(4);
        assertThat(recorder.terminals).singleElement().satisfies(terminal ->
            assertThat(terminal.attempts()).isEqualTo(2));
    }

    @Test
    void registryConflictsAndRequiredAdapterAbsenceFailContextRefresh() {
        try (AnnotationConfigApplicationContext empty = new AnnotationConfigApplicationContext()) {
            empty.registerBean(ThirdEndpointConfigStore.class, () -> new EndpointStore(List.of()));
            empty.registerBean(ThirdProviderAdapterRegistry.class);
            empty.registerBean(ThirdProviderAdapterStartupValidator.class);
            assertThatCode(empty::refresh).doesNotThrowAnyException();
        }

        try (AnnotationConfigApplicationContext duplicate = new AnnotationConfigApplicationContext()) {
            duplicate.registerBean("firstAdapter", ThirdProviderAdapter.class,
                () -> new FixedAdapter("duplicate", "one"));
            duplicate.registerBean("secondAdapter", ThirdProviderAdapter.class,
                () -> new FixedAdapter("duplicate", "two"));
            duplicate.registerBean(ThirdProviderAdapterRegistry.class);
            assertThatThrownBy(duplicate::refresh)
                .hasRootCauseMessage("Duplicate third provider adapter: duplicate");
        }

        ThirdEndpoint configured = endpoint("/json", "JSON");
        configured.setProviderCode("missing-provider");
        configured.setAdapterCode("required-signature");
        try (AnnotationConfigApplicationContext missing = new AnnotationConfigApplicationContext()) {
            missing.registerBean(ThirdEndpointConfigStore.class, () -> new EndpointStore(List.of(configured)));
            missing.registerBean(ThirdProviderAdapterRegistry.class);
            missing.registerBean(ThirdProviderAdapterStartupValidator.class);
            assertThatThrownBy(missing::refresh)
                .hasMessage("Third endpoint adapter is unavailable");
        }
    }

    private ThirdEndpoint endpoint(String path, String responseMode) {
        ThirdEndpoint endpoint = new ThirdEndpoint();
        endpoint.setEndpointId(2L);
        endpoint.setProviderId(1L);
        endpoint.setProviderCode("qichacha");
        endpoint.setEndpointCode("company");
        endpoint.setHttpMethod("GET");
        endpoint.setRelativePath(path);
        endpoint.setRequestMode("QUERY");
        endpoint.setResponseMode(responseMode);
        endpoint.setPathSchemaJson("{\"allowed\":[]}");
        endpoint.setQuerySchemaJson("{\"allowed\":[]}");
        endpoint.setHeaderSchemaJson("{\"allowed\":[]}");
        endpoint.setBodySchemaJson("{\"allowed\":[]}");
        endpoint.setResponseSchemaJson("{}");
        endpoint.setOverrideJson("{}");
        endpoint.setSensitiveFieldsJson("[]");
        endpoint.setStatus("0");
        endpoint.setIdempotent(false);
        endpoint.setRetryCount(0);
        return endpoint;
    }

    private void configure(String path, String responseMode, boolean idempotent, int retries) {
        snapshot.getEndpoint().setRelativePath(path);
        snapshot.getEndpoint().setResponseMode(responseMode);
        snapshot.getEndpoint().setIdempotent(idempotent);
        snapshot.getEndpoint().setRetryCount(retries);
    }

    private ThirdPartyRequest request() {
        return ThirdPartyRequest.of("qichacha", "company");
    }

    private int counted(HttpExchange exchange) {
        return requests.computeIfAbsent(exchange.getRequestURI().getPath(), ignored -> new AtomicInteger())
            .incrementAndGet();
    }

    private int count(String path) {
        return requests.getOrDefault(path, new AtomicInteger()).get();
    }

    private int totalRequests() {
        return requests.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    private void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        counted(exchange);
        write(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        counted(exchange);
        write(exchange, status, contentType, body);
    }

    private void delayed(HttpExchange exchange, long millis, String body) {
        counted(exchange);
        sleep(millis);
        try {
            write(exchange, 200, "application/json", body.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            exchange.close();
        }
    }

    private static void write(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class SnapshotPort implements ThirdConfigSnapshotPort {
        private final ThirdConfigSnapshot snapshot;

        private SnapshotPort(ThirdConfigSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public ThirdConfigSnapshot get(String providerCode, String endpointCode) {
            return snapshot;
        }

        @Override
        public void evict(String providerCode, String endpointCode) { }
    }

    private static final class TestResilience implements ThirdResiliencePort {
        @Override
        public ThirdLimitLease acquire(ThirdProvider provider, ThirdEndpoint endpoint) {
            return () -> { };
        }

        @Override
        public int maxAttempts(ThirdEndpoint endpoint) {
            return Boolean.TRUE.equals(endpoint.getIdempotent())
                ? 1 + Math.min(Math.max(endpoint.getRetryCount(), 0), 3) : 1;
        }
    }

    private static final class RecordingInvocationRecorder implements ThirdInvocationRecorderPort {
        private final List<Terminal> terminals = new ArrayList<>();
        private final List<ThirdOutboundAttempt> attempts = new ArrayList<>();

        @Override
        public void record(ThirdPartyRequest request, ThirdPartyResponse<?> response, long durationMs, int attempts) {
            terminals.add(new Terminal(response.category(), attempts));
        }

        @Override
        public void recordAttempt(ThirdOutboundAttempt attempt) {
            attempts.add(attempt);
        }

        private void clear() {
            terminals.clear();
            attempts.clear();
        }
    }

    private static final class BusinessAdapter implements ThirdProviderAdapter {
        @Override
        public String providerCode() {
            return "qichacha";
        }

        @Override
        public ThirdPartyResponse<?> mapResponse(ThirdAdapterResponse response) {
            if (response.body() instanceof JsonNode body && body.has("code")) {
                return new ThirdPartyResponse<>(response.requestId(), response.request().providerCode(),
                    response.request().endpointCode(), response.httpStatus(), ThirdPartyFailureCategory.PROVIDER,
                    body.path("message").asText(), null);
            }
            return null;
        }
    }

    private record FixedAdapter(String providerCode, String adapterCode) implements ThirdProviderAdapter { }

    private static final class NoopCrypto implements ThirdCredentialCryptoPort {
        @Override
        public EncryptedSecret encrypt(String scopeType, String credentialType, String json) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String decrypt(ThirdCredential credential) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class EndpointStore implements ThirdEndpointConfigStore {
        private final List<ThirdEndpoint> configured;

        private EndpointStore(List<ThirdEndpoint> configured) {
            this.configured = configured;
        }

        @Override
        public ThirdEndpoint findActiveByProviderAndCode(Long providerId, String endpointCode) {
            return null;
        }

        @Override
        public List<ThirdEndpoint> findAllByProviderCode(String providerCode) {
            return List.of();
        }

        @Override
        public List<ThirdEndpoint> findAllWithAdapter() {
            return configured;
        }
    }

    private record Payload(String result) { }

    private record Terminal(ThirdPartyFailureCategory category, int attempts) { }
}
