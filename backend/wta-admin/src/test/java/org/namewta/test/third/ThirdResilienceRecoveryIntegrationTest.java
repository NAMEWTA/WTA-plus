package org.namewta.test.third;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.namewta.third.adapter.resilience.ThirdResiliencePolicyAdapter;
import org.namewta.third.api.ThirdPartyFailureCategory;
import org.namewta.third.domain.ThirdEndpoint;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.support.ThirdLimitLease;
import org.namewta.third.support.ThirdRejectedException;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Only the disposable loopback Redis supplied by the change runner is used. */
@Tag("dev")
public class ThirdResilienceRecoveryIntegrationTest {
    @TempDir Path temporary;
    private RedissonClient client;
    private ThirdResiliencePolicyAdapter policy;
    private String port;
    private String code;

    @BeforeEach
    void connect() {
        port = System.getProperty("third.redis.integration.port");
        assumeTrue(port != null && !port.isBlank(), "requires owned Redis");
        client = client(port);
        policy = new ThirdResiliencePolicyAdapter(client);
        code = "t24-" + UUID.randomUUID().toString().replace("-", "");
    }

    @AfterEach
    void close() {
        if (client != null) client.shutdown();
    }

    @Test
    void killedProcessPermitExpiresWithoutDeletingKeys() throws Exception {
        Path ready = temporary.resolve("ready");
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
            ThirdResilienceRecoveryIntegrationTest.class.getName(), port, code, ready.toString())
            .redirectErrorStream(true).redirectOutput(temporary.resolve("child.log").toFile()).start();
        try {
            await().atMost(Duration.ofSeconds(15)).until(() -> Files.exists(ready));
            reject(provider(code, 0, 1, 0), endpoint("get", 0, 0), ThirdPartyFailureCategory.REJECTED);
            child.destroyForcibly();
            assertThat(child.waitFor(5, TimeUnit.SECONDS)).isTrue();
            // 100 ms connect + 100 ms read + the bounded local handoff allowance.
            await().ignoreException(ThirdRejectedException.class).pollInterval(Duration.ofMillis(100))
                .atMost(Duration.ofSeconds(4)).untilAsserted(() -> {
                try (ThirdLimitLease recovered = policy.acquire(provider(code, 0, 1, 0), endpoint("get", 0, 0))) {
                    assertThat(recovered).isNotNull();
                }
            });
        } finally {
            if (child.isAlive()) { child.destroyForcibly(); child.waitFor(5, TimeUnit.SECONDS); }
        }
    }

    @Test
    void loweringConcurrencyWaitsForExistingOwnersAcrossClients() {
        ThirdProvider original = provider(code, 0, 2, 0);
        ThirdEndpoint endpoint = endpoint("get", 0, 0);
        ThirdLimitLease first = policy.acquire(original, endpoint);
        ThirdLimitLease second = policy.acquire(original, endpoint);
        RedissonClient other = client(port);
        try {
            ThirdResiliencePolicyAdapter peer = new ThirdResiliencePolicyAdapter(other);
            ThirdProvider lower = provider(code, 0, 1, 1);
            assertThatThrownBy(() -> peer.acquire(lower, endpoint)).isInstanceOf(ThirdRejectedException.class);
            first.close();
            assertThatThrownBy(() -> peer.acquire(lower, endpoint))
                .isInstanceOfSatisfying(ThirdRejectedException.class,
                    e -> assertThat(e.category()).isEqualTo(ThirdPartyFailureCategory.REJECTED));
            second.close();
            try (ThirdLimitLease recovered = peer.acquire(lower, endpoint)) { assertThat(recovered).isNotNull(); }
        } finally { first.close(); second.close(); other.shutdown(); }
    }

    @Test
    void endpointRejectionReturnsProviderPermitAndLateCloseCannotReleaseNewOwner() {
        ThirdProvider provider = provider(code, 0, 2, 0);
        ThirdEndpoint endpoint = endpoint("get", 0, 1);
        ThirdLimitLease first = policy.acquire(provider, endpoint);
        reject(provider, endpoint, ThirdPartyFailureCategory.REJECTED);
        assertThat(client.getPermitExpirableSemaphore("third:concurrency:provider:" + code).acquiredPermits()).isEqualTo(1);
        first.close();
        ThirdLimitLease next = policy.acquire(provider, endpoint);
        first.close();
        reject(provider, endpoint, ThirdPartyFailureCategory.REJECTED);
        next.close();
        try (ThirdLimitLease finalLease = policy.acquire(provider, endpoint)) { assertThat(finalLease).isNotNull(); }
    }

    @Test
    void expiredOwnerCannotReleaseReplacementPermit() {
        ThirdProvider provider = provider(code, 0, 1, 0);
        ThirdEndpoint endpoint = endpoint("get", 0, 0);
        ThirdLimitLease expired = policy.acquire(provider, endpoint);
        await().atMost(Duration.ofSeconds(4)).until(() ->
            client.getPermitExpirableSemaphore("third:concurrency:provider:" + code).availablePermits() == 1);
        ThirdLimitLease replacement = policy.acquire(provider, endpoint);
        try {
            expired.close();
            reject(provider, endpoint, ThirdPartyFailureCategory.REJECTED);
        } finally { replacement.close(); }
    }

    @Test
    void increasingRateKeepsSpentRequestsAndOldSnapshotCannotRestoreQuota() {
        ThirdEndpoint endpoint = endpoint("get", 0, 0);
        ThirdProvider original = provider(code, 1, 0, 0);
        policy.acquire(original, endpoint).close();
        ThirdProvider raised = provider(code, 2, 0, 1);
        RedissonClient other = client(port);
        try {
            ThirdResiliencePolicyAdapter peer = new ThirdResiliencePolicyAdapter(other);
            peer.acquire(raised, endpoint).close();
            reject(raised, endpoint, ThirdPartyFailureCategory.RATE_LIMITED);
            reject(original, endpoint, ThirdPartyFailureCategory.CONFIG_UNAVAILABLE);
            assertThat(client.getRateLimiter("third:rate:provider:" + code).getConfig().getRate()).isEqualTo(2);
        } finally { other.shutdown(); }
    }

    @Test
    void loweringRateCannotGrantFromPartialExpiryOfTheOldWindow() throws Exception {
        ThirdEndpoint endpoint = endpoint("get", 0, 0);
        ThirdProvider original = provider(code, 3, 0, 0);
        policy.acquire(original, endpoint).close();
        Thread.sleep(450);
        policy.acquire(original, endpoint).close();
        ThirdProvider lower = provider(code, 1, 0, 1);
        reject(lower, endpoint, ThirdPartyFailureCategory.RATE_LIMITED);
        Thread.sleep(650); // First old request expired, second is still in the one-second window.
        reject(lower, endpoint, ThirdPartyFailureCategory.RATE_LIMITED);
        Thread.sleep(450);
        policy.acquire(lower, endpoint).close();
        reject(lower, endpoint, ThirdPartyFailureCategory.RATE_LIMITED);
    }

    @Test
    void changingUnlimitedToOneCountsInflightCallsAndEndpointLimitsRefresh() {
        ThirdProvider original = provider(code, 0, 0, 0);
        ThirdEndpoint endpoint = endpoint("get", 0, 0);
        ThirdLimitLease first = policy.acquire(original, endpoint);
        ThirdLimitLease second = policy.acquire(original, endpoint);
        ThirdProvider lower = provider(code, 0, 1, 1);
        policy.refresh(lower, null);
        reject(lower, endpoint, ThirdPartyFailureCategory.REJECTED);
        first.close();
        reject(lower, endpoint, ThirdPartyFailureCategory.REJECTED);
        second.close();
        ThirdProvider raised = provider(code, 0, 2, 2);
        endpoint.setConcurrencyLimit(1); endpoint.setVersion(1);
        policy.refresh(raised, endpoint);
        try (ThirdLimitLease lease = policy.acquire(raised, endpoint)) {
            assertThat(lease).isNotNull();
            reject(raised, endpoint, ThirdPartyFailureCategory.REJECTED);
        }
    }

    @Test
    void redisFailureIsClosedEvenWhenBothLimitsAreUnlimited() {
        client.shutdown();
        reject(provider(code, 0, 0, 0), endpoint("get", 0, 0), ThirdPartyFailureCategory.CONFIG_UNAVAILABLE);
    }

    @Test
    void interruptedConfigurationUpdateIsRepairedWithoutRestoringAnOldQuota() {
        ThirdProvider original = provider(code, 3, 3, 0);
        ThirdEndpoint endpoint = endpoint("get", 0, 0);
        policy.acquire(original, endpoint).close();
        RedissonClient faultyClient = org.mockito.Mockito.spy(client);
        var limiter = org.mockito.Mockito.spy(client.getRateLimiter("third:rate:provider:" + code));
        org.mockito.Mockito.doReturn(limiter).when(faultyClient).getRateLimiter("third:rate:provider:" + code);
        org.mockito.Mockito.doThrow(new IllegalStateException("owned interruption"))
            .doCallRealMethod().when(limiter).setRate(org.mockito.ArgumentMatchers.any(org.redisson.api.ratelimiter.RateLimiterArgs.class));
        ThirdProvider lower = provider(code, 2, 1, 1);
        assertThatThrownBy(() -> new ThirdResiliencePolicyAdapter(faultyClient).refresh(lower, null))
            .isInstanceOf(ThirdRejectedException.class);
        assertThat(client.<String>getBucket("third:limit-config:provider:" + code, StringCodec.INSTANCE).get()).endsWith(":pending");
        reject(original, endpoint, ThirdPartyFailureCategory.CONFIG_UNAVAILABLE);
        policy.refresh(lower, null);
        assertThat(client.<String>getBucket("third:limit-config:provider:" + code, StringCodec.INSTANCE).get()).endsWith(":ready");
        assertThat(client.getPermitExpirableSemaphore("third:concurrency:provider:" + code).getPermits()).isEqualTo(1);
        assertThat(client.getRateLimiter("third:rate:provider:" + code).getConfig().getRate()).isEqualTo(2);
    }

    @Test
    void pausedOwnedRedisRejectsRequestsWithoutLeakingTheConnectionFailure() throws Exception {
        String container = System.getProperty("third.redis.integration.container");
        assertThat(container).isNotBlank();
        var inspect = new ProcessBuilder("docker", "inspect", "--format", "{{index .Config.Labels \"namewta.test.owner\"}}", container).start();
        assertThat(new String(inspect.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim()).isEqualTo("T-24");
        assertThat(inspect.waitFor()).isZero();
        Config config = new Config(); config.setCodec(StringCodec.INSTANCE); config.setThreads(2); config.setNettyThreads(2);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port).setTimeout(200).setRetryAttempts(0)
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4);
        RedissonClient bounded = Redisson.create(config);
        try {
            var pause = new ProcessBuilder("docker", "exec", container, "redis-cli", "CLIENT", "PAUSE", "1500").start();
            assertThat(pause.waitFor()).isZero();
            assertThatThrownBy(() -> new ThirdResiliencePolicyAdapter(bounded)
                .acquire(provider(code, 1, 1, 0), endpoint("get", 0, 0)))
                .isInstanceOfSatisfying(ThirdRejectedException.class, error -> {
                    assertThat(error.category()).isEqualTo(ThirdPartyFailureCategory.CONFIG_UNAVAILABLE);
                    assertThat(error.getMessage()).isEqualTo("Third-party limit service unavailable");
                });
        } finally {
            var resume = new ProcessBuilder("docker", "exec", container, "redis-cli", "CLIENT", "UNPAUSE").start();
            assertThat(resume.waitFor()).isZero(); bounded.shutdown();
        }
    }

    @Test
    void realHttpSuccessFailureAndBoundedRetriesReleaseBothPermits() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        var mode = new java.util.concurrent.atomic.AtomicInteger();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        server.setExecutor(executor);
        server.createContext("/get", exchange -> {
            calls.incrementAndGet();
            try (exchange) {
                if (mode.get() == 2) {
                    try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(mode.get() == 1 ? 503 : 200, 2);
                exchange.getResponseBody().write("ok".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.io.IOException expectedAfterTimeout) {
                // The real client has already cancelled the timed-out response.
            }
        });
        server.start();
        try {
            ThirdProvider provider = provider(code, 0, 1, 0);
            provider.setStatus("0"); provider.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            ThirdEndpoint endpoint = endpoint("get", 0, 1);
            endpoint.setProviderId(provider.getProviderId()); endpoint.setProviderCode(code); endpoint.setStatus("0");
            endpoint.setRelativePath("/get"); endpoint.setHttpMethod("GET"); endpoint.setRequestMode("QUERY"); endpoint.setResponseMode("TEXT");
            endpoint.setIdempotent(true); endpoint.setRetryCount(3);
            var snapshots = org.mockito.Mockito.mock(org.namewta.third.port.ThirdConfigSnapshotPort.class);
            org.mockito.Mockito.when(snapshots.get(code, "get")).thenReturn(new org.namewta.third.port.ThirdConfigSnapshot(provider, endpoint));
            var gateway = new org.namewta.third.adapter.gateway.ThirdGatewayAdapter(snapshots,
                new org.namewta.third.spi.ThirdProviderAdapterRegistry(java.util.List.of()), policy,
                org.mockito.Mockito.mock(org.namewta.third.port.ThirdInvocationRecorderPort.class),
                (providerId, endpointId) -> java.util.List.of(), org.mockito.Mockito.mock(org.namewta.third.port.ThirdCredentialCryptoPort.class),
                new org.namewta.third.http.ThirdHttpClientFactory());
            var request = org.namewta.third.api.ThirdPartyRequest.of(code, "get");
            assertThat(gateway.execute(request).category()).isEqualTo(ThirdPartyFailureCategory.NONE);
            assertNoAcquiredPermits();
            mode.set(1);
            assertThat(gateway.execute(request).category()).isEqualTo(ThirdPartyFailureCategory.HTTP);
            assertNoAcquiredPermits();
            int beforeTimeout = calls.get(); mode.set(2);
            assertThat(gateway.execute(request).category()).isEqualTo(ThirdPartyFailureCategory.TIMEOUT);
            assertThat(calls.get() - beforeTimeout).isEqualTo(4);
            assertNoAcquiredPermits();
        } finally { server.stop(0); executor.shutdownNow(); executor.close(); }
    }

    private void assertNoAcquiredPermits() {
        assertThat(client.getPermitExpirableSemaphore("third:concurrency:provider:" + code).acquiredPermits()).isZero();
        assertThat(client.getPermitExpirableSemaphore("third:concurrency:endpoint:" + code + ":get").acquiredPermits()).isZero();
    }

    private void reject(ThirdProvider provider, ThirdEndpoint endpoint, ThirdPartyFailureCategory category) {
        assertThatThrownBy(() -> policy.acquire(provider, endpoint)).isInstanceOfSatisfying(ThirdRejectedException.class,
            e -> assertThat(e.category()).isEqualTo(category));
    }

    private static ThirdProvider provider(String code, int rate, int concurrency, int version) {
        ThirdProvider provider = new ThirdProvider();
        provider.setProviderCode(code); provider.setProviderId(24001L); provider.setVersion(version);
        provider.setRateLimit(rate); provider.setConcurrencyLimit(concurrency);
        provider.setTimeoutConnectMs(100); provider.setTimeoutReadMs(100);
        return provider;
    }

    private static ThirdEndpoint endpoint(String code, int rate, int concurrency) {
        ThirdEndpoint endpoint = new ThirdEndpoint();
        endpoint.setEndpointCode(code); endpoint.setEndpointId(24002L); endpoint.setVersion(0);
        endpoint.setRateLimit(rate); endpoint.setConcurrencyLimit(concurrency);
        return endpoint;
    }

    private static RedissonClient client(String port) {
        Config config = new Config(); config.setCodec(StringCodec.INSTANCE);
        config.setThreads(2); config.setNettyThreads(2);
        config.useSingleServer().setAddress("redis://127.0.0.1:" + port)
            .setConnectionMinimumIdleSize(1).setConnectionPoolSize(4)
            .setSubscriptionConnectionMinimumIdleSize(1).setSubscriptionConnectionPoolSize(2);
        return Redisson.create(config);
    }

    public static void main(String[] args) throws Exception {
        RedissonClient client = client(args[0]);
        new ThirdResiliencePolicyAdapter(client).acquire(provider(args[1], 0, 1, 0), endpoint("get", 0, 0));
        Files.writeString(Path.of(args[2]), "owned");
        Thread.sleep(30000); // Parent forcibly kills this owned JVM, bypassing finally/release.
        client.shutdown();
    }
}
