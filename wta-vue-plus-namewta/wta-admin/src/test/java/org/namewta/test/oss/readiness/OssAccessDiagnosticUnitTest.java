package org.namewta.common.oss.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssAccessDiagnostic;
import org.namewta.common.oss.model.OssClientCapabilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class OssAccessDiagnosticUnitTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void verifiesPublicReadUsingPolicyAndAnonymousHeadGet() throws Exception {
        startServer(200, 206);
        S3AsyncClient s3 = provider(publicReadPolicy(), completedAcl());

        OssAccessDiagnostic result = client(s3).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));

        assertThat(result.verified()).isTrue();
        assertThat(result.reason()).isEqualTo(OssAccessDiagnostic.Reason.READY);
        assertThat(result.anonymousHeadAllowed()).isTrue();
        assertThat(result.anonymousGetAllowed()).isTrue();
        assertThat(result.anonymousWriteDenied()).isTrue();
    }

    @Test
    void acceptsSingleStatementObjectPolicy() throws Exception {
        startServer(200, 206);
        String objectPolicy = "{\"Statement\":{"
            + "\"Effect\":\"Allow\",\"Principal\":\"*\","
            + "\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::bucket/*\"}}";

        OssAccessDiagnostic result = client(provider(objectPolicy, completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));

        assertThat(result.verified()).isTrue();
    }

    @Test
    void verifiesPrivateWhenPolicyIsAbsentAndAnonymousReadIsDenied() throws Exception {
        startServer(403, 403);
        S3AsyncClient s3 = provider(null, completedAcl());

        OssAccessDiagnostic result = client(s3).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));

        assertThat(result.verified()).isTrue();
        assertThat(result.anonymousHeadAllowed()).isFalse();
        assertThat(result.anonymousGetAllowed()).isFalse();
        assertThat(result.anonymousWriteDenied()).isTrue();
    }

    @Test
    void rejectsAnonymousWritePolicyAndContradictoryReadObservation() throws Exception {
        startServer(200, 206);
        OssAccessDiagnostic write = client(provider(publicReadWritePolicy(), completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));
        assertThat(write.verification()).isEqualTo(OssAccessDiagnostic.Verification.MISMATCH);
        assertThat(write.reason()).isEqualTo(OssAccessDiagnostic.Reason.ANONYMOUS_WRITE_ALLOWED);

        server.stop(0);
        server = null;
        startServer(403, 403);
        OssAccessDiagnostic contradiction = client(provider(publicReadPolicy(), completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));
        assertThat(contradiction.verification()).isEqualTo(OssAccessDiagnostic.Verification.MISMATCH);
        assertThat(contradiction.reason()).isEqualTo(OssAccessDiagnostic.Reason.ANONYMOUS_READ_MISMATCH);
    }

    @Test
    void timeoutIsUnverifiedAndDoesNotLeakProviderDetails() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        when(s3.headObject(any(Consumer.class))).thenReturn(new CompletableFuture<>());

        OssAccessDiagnostic result = client(s3).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofMillis(50));

        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(result.reason()).isEqualTo(OssAccessDiagnostic.Reason.TIMEOUT);
        assertThat(result.toString()).doesNotContain("access-key", "secret-key", "http://");
    }

    @Test
    void unsupportedProviderAndUnreadablePolicyFailClosed() throws Exception {
        startServer(403, 403);
        DiagnosticClient unsupported = client(provider(null, completedAcl()), false);

        OssAccessDiagnostic unsupportedResult = unsupported.diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));

        assertThat(unsupportedResult.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(unsupportedResult.reason()).isEqualTo(OssAccessDiagnostic.Reason.UNSUPPORTED);

        S3AsyncClient denied = mock(S3AsyncClient.class);
        when(denied.headObject(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().contentLength(1L).build()));
        S3Exception.Builder builder = S3Exception.builder();
        builder.statusCode(403);
        builder.message("sensitive provider detail");
        when(denied.getBucketPolicy(any(Consumer.class)))
            .thenReturn(CompletableFuture.failedFuture(builder.build()));

        OssAccessDiagnostic deniedResult = client(denied, true).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));

        assertThat(deniedResult.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(deniedResult.reason()).isEqualTo(OssAccessDiagnostic.Reason.POLICY_UNREADABLE);
        assertThat(deniedResult.toString()).doesNotContain("sensitive provider detail");
    }

    private S3AsyncClient provider(String policy, CompletableFuture<GetBucketAclResponse> acl) {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        when(s3.headObject(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().contentLength(1L).build()));
        if (policy == null) {
            S3Exception.Builder builder = S3Exception.builder();
            builder.statusCode(404);
            builder.message("NoSuchBucketPolicy");
            S3Exception noPolicy = (S3Exception) builder.build();
            when(s3.getBucketPolicy(any(Consumer.class))).thenReturn(CompletableFuture.failedFuture(noPolicy));
        } else {
            when(s3.getBucketPolicy(any(Consumer.class))).thenReturn(CompletableFuture.completedFuture(
                GetBucketPolicyResponse.builder().policy(policy).build()));
        }
        when(s3.getBucketAcl(any(Consumer.class))).thenReturn(acl);
        return s3;
    }

    private CompletableFuture<GetBucketAclResponse> completedAcl() {
        return CompletableFuture.completedFuture(GetBucketAclResponse.builder().grants(List.of()).build());
    }

    private DiagnosticClient client(S3AsyncClient s3) {
        return client(s3, true);
    }

    private DiagnosticClient client(S3AsyncClient s3, boolean diagnosticSupported) {
        DiagnosticClient.initializingClient = s3;
        return new DiagnosticClient(OssClientConfig.builder()
            .endpoint("127.0.0.1:" + (server == null ? 1 : server.getAddress().getPort()))
            .useHttps(false)
            .usePathStyleAccess(true)
            .accessKey("access-key")
            .secretKey("secret-key")
            .bucket("bucket")
            .region(Region.US_EAST_1)
            .prefix("")
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT)
            .build(), diagnosticSupported);
    }

    private void startServer(int headStatus, int getStatus) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bucket/diagnostic/canary.txt", exchange ->
            respond(exchange, "HEAD".equals(exchange.getRequestMethod()) ? headStatus : getStatus));
        server.start();
    }

    private void respond(HttpExchange exchange, int status) throws IOException {
        byte[] body = "x".getBytes(StandardCharsets.UTF_8);
        if ("HEAD".equals(exchange.getRequestMethod()) || status >= 400) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
        exchange.close();
    }

    private String publicReadPolicy() {
        return "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\","
            + "\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::bucket/*\"}]}";
    }

    private String publicReadWritePolicy() {
        return "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\","
            + "\"Action\":[\"s3:GetObject\",\"s3:PutObject\"],"
            + "\"Resource\":\"arn:aws:s3:::bucket/*\"}]}";
    }

    private static final class DiagnosticClient extends AbstractOssClientImpl {
        private static S3AsyncClient initializingClient;
        private final boolean diagnosticSupported;

        private DiagnosticClient(OssClientConfig config, boolean diagnosticSupported) {
            super("diagnostic-test", config);
            this.diagnosticSupported = diagnosticSupported;
        }

        @Override
        void doInitialize() {
            s3AsyncClient = initializingClient;
        }

        @Override
        public OssClientCapabilities capabilities() {
            return new OssClientCapabilities(true, true, java.util.Set.of(), diagnosticSupported);
        }
    }
}
