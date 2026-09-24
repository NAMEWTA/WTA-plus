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
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class OssAccessDiagnosticUnitTest {

    private HttpServer server;
    private final List<String> observedMethods = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void observesPublicReadUsingPolicyAndAnonymousHeadGetWithoutAssumingWriteSafety() throws Exception {
        startServer(200, 206);
        S3AsyncClient s3 = provider(publicReadPolicy(), completedAcl());

        OssAccessDiagnostic result = client(s3).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));

        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.POLICY_READ))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_HEAD))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.POLICY_WRITE))
            .isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
    }

    @Test
    void acceptsSingleStatementObjectPolicy() throws Exception {
        startServer(200, 206);
        String objectPolicy = "{\"Statement\":{"
            + "\"Effect\":\"Allow\",\"Principal\":\"*\","
            + "\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::bucket/*\"}}";

        OssAccessDiagnostic result = client(provider(objectPolicy, completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));

        assertThat(observation(result, OssAccessDiagnostic.Subject.POLICY_READ))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
    }

    @Test
    void observesPrivateObject403WithoutClaimingWholeBucketSafety() throws Exception {
        startServer(403, 403);
        S3AsyncClient s3 = provider(null, completedAcl());

        OssAccessDiagnostic result = client(s3).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));

        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_HEAD))
            .isEqualTo(OssAccessDiagnostic.Observation.DENIED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.DENIED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.POLICY_WRITE))
            .isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
        assertThat(result.facts()).filteredOn(fact -> fact.subject() == OssAccessDiagnostic.Subject.POLICY_READ)
            .extracting(OssAccessDiagnostic.Fact::basis)
            .containsExactly(OssAccessDiagnostic.Basis.NO_SUCH_POLICY);
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
        var deniedError = builder.build();
        when(denied.getBucketPolicy(any(Consumer.class)))
            .thenReturn(CompletableFuture.failedFuture(deniedError));
        when(denied.getBucketAcl(any(Consumer.class)))
            .thenReturn(CompletableFuture.failedFuture(deniedError));

        OssAccessDiagnostic deniedResult = client(denied, true).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));

        assertThat(deniedResult.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(observation(deniedResult, OssAccessDiagnostic.Subject.OBJECT_HEAD))
            .isEqualTo(OssAccessDiagnostic.Observation.DENIED);
        assertThat(observation(deniedResult, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.DENIED);
        assertThat(deniedResult.toString()).doesNotContain("sensitive provider detail");

        S3AsyncClient broken = mock(S3AsyncClient.class);
        when(broken.headObject(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().contentLength(1L).build()));
        S3Exception.Builder serverError = S3Exception.builder();
        serverError.statusCode(500);
        serverError.message("sensitive provider detail");
        when(broken.getBucketPolicy(any(Consumer.class)))
            .thenReturn(CompletableFuture.failedFuture(serverError.build()));

        OssAccessDiagnostic brokenResult = client(broken, true).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));

        assertThat(brokenResult.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(brokenResult.reason()).isEqualTo(OssAccessDiagnostic.Reason.INSUFFICIENT_EVIDENCE);
        assertThat(brokenResult.facts()).filteredOn(fact ->
            fact.source() == OssAccessDiagnostic.Source.BUCKET_POLICY).allSatisfy(fact -> {
                assertThat(fact.observation()).isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
                assertThat(fact.basis()).isEqualTo(OssAccessDiagnostic.Basis.HTTP_ERROR);
            });
        assertThat(observation(brokenResult, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.DENIED);
        assertThat(brokenResult.toString()).doesNotContain("sensitive provider detail");
    }

    @Test
    void unreadablePolicyAndAclDoNotContradictObservedPublicObjectRead() throws Exception {
        startServer(200, 206);

        OssAccessDiagnostic result = client(unreadablePolicyAndAcl()).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));

        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_HEAD))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(result.reason()).isNotEqualTo(OssAccessDiagnostic.Reason.POLICY_MISMATCH);
        assertThat(observation(result, OssAccessDiagnostic.Subject.POLICY_WRITE))
            .isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
    }

    @Test
    void privateObject403AndUnreadablePolicyDoNotProveBucketWideSafety() throws Exception {
        startServer(403, 403);

        OssAccessDiagnostic result = client(unreadablePolicyAndAcl()).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));

        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_HEAD))
            .isEqualTo(OssAccessDiagnostic.Observation.DENIED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.DENIED);
        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.POLICY_WRITE))
            .isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
    }

    @Test
    void denyDeleteDoesNotHideAnAllowedPutDeclaration() throws Exception {
        startServer(403, 403);
        String policy = "{\"Statement\":["
            + "{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"s3:PutObject\","
            + "\"Resource\":\"arn:aws:s3:::bucket/*\"},"
            + "{\"Effect\":\"Deny\",\"Principal\":\"*\",\"Action\":\"s3:DeleteObject\","
            + "\"Resource\":\"arn:aws:s3:::bucket/*\"}]}";
        OssAccessDiagnostic result = client(provider(policy, completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));
        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.MISMATCH);
        assertThat(result.reason()).isEqualTo(OssAccessDiagnostic.Reason.ANONYMOUS_WRITE_ALLOWED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.POLICY_WRITE))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
    }

    @Test
    void emptySuccessfulPolicyDocumentIsInvalidRatherThanConfirmedMissing() throws Exception {
        startServer(403, 403);
        OssAccessDiagnostic result = client(provider("", completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));
        assertThat(result.facts()).filteredOn(fact -> fact.subject() == OssAccessDiagnostic.Subject.POLICY_READ)
            .extracting(OssAccessDiagnostic.Fact::basis)
            .containsExactly(OssAccessDiagnostic.Basis.INVALID_POLICY);
    }

    @Test
    void missingHeadObjectDoesNotEraseIndependentSuccessfulGetFactOrPerformWrites() throws Exception {
        startServer(404, 206);
        OssAccessDiagnostic result = client(provider(publicReadPolicy(), completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_HEAD))
            .isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
        assertThat(observedMethods).containsExactly("HEAD", "GET");
    }

    @Test
    void conditionalPolicyIsUnknownEvenWhenAnonymousObjectGetSucceeds() throws Exception {
        startServer(200, 206);
        String conditional = publicReadPolicy().replace("\"Action\"", "\"Condition\":{\"Bool\":{\"aws:SecureTransport\":\"true\"}},\"Action\"");
        OssAccessDiagnostic result = client(provider(conditional, completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));
        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(result.facts()).filteredOn(fact -> fact.subject() == OssAccessDiagnostic.Subject.POLICY_READ)
            .extracting(OssAccessDiagnostic.Fact::basis)
            .containsExactly(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
    }

    @Test
    void apiOperationNameIsNotMistakenForAnIamPolicyAction() throws Exception {
        startServer(403, 403);
        String unsupportedAction = publicReadPolicy().replace("s3:GetObject", "s3:UploadPart");
        OssAccessDiagnostic result = client(provider(unsupportedAction, completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));
        assertThat(result.facts()).filteredOn(fact -> fact.subject() == OssAccessDiagnostic.Subject.POLICY_WRITE)
            .extracting(OssAccessDiagnostic.Fact::basis)
            .containsExactly(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
    }

    @Test
    void unsupportedPolicyShapesNeverBecomeAConfirmedWriteGrantOrDenial() throws Exception {
        startServer(403, 403);
        String allowedWrite = publicReadWritePolicy();
        List<String> unsupported = List.of(
            allowedWrite.replace("bucket/*", "other-bucket/*"),
            allowedWrite.replace("bucket/*", "bucket/other-prefix/*"),
            allowedWrite.replace("\"Action\"", "\"Condition\":{\"Bool\":{\"aws:SecureTransport\":\"true\"}},\"Action\""),
            allowedWrite.replace("\"Action\"", "\"NotAction\""),
            allowedWrite.replace("\"Principal\":\"*\"", "\"Principal\":\"another-account\""),
            "{bad-json"
        );
        for (String policy : unsupported) {
            OssAccessDiagnostic result = client(provider(policy, completedAcl())).diagnoseAccess(
                "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));
            assertThat(observation(result, OssAccessDiagnostic.Subject.POLICY_WRITE))
                .as("unsupported policy shape %s", unsupported.indexOf(policy))
                .isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
            assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        }
    }

    @Test
    void redirectsAndServerErrorsRemainUnknownWithoutFollowingOrReadingObjectData() throws Exception {
        startServer(302, 500);
        OssAccessDiagnostic result = client(provider(publicReadPolicy(), completedAcl())).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));
        assertThat(result.facts()).filteredOn(fact -> fact.subject() == OssAccessDiagnostic.Subject.OBJECT_HEAD)
            .extracting(OssAccessDiagnostic.Fact::basis).containsExactly(OssAccessDiagnostic.Basis.REDIRECT);
        assertThat(result.facts()).filteredOn(fact -> fact.subject() == OssAccessDiagnostic.Subject.OBJECT_GET)
            .extracting(OssAccessDiagnostic.Fact::basis).containsExactly(OssAccessDiagnostic.Basis.HTTP_ERROR);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_HEAD))
            .isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN);
        assertThat(observedMethods).containsExactly("HEAD", "GET");
    }

    @Test
    void anonymousRangeProbeReadsHeadersWithoutWaitingForAnUnboundedObjectBody() throws Exception {
        CountDownLatch releaseBody = new CountDownLatch(1);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bucket/diagnostic/canary.txt", exchange -> {
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(206, 65_536);
            try {
                exchange.getResponseBody().write('x');
                exchange.getResponseBody().flush();
                releaseBody.await(8, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            long started = System.nanoTime();
            OssAccessDiagnostic result = client(provider(publicReadPolicy(), completedAcl())).diagnoseAccess(
                "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofMillis(300));
            assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_GET))
                .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(4));
        } finally {
            releaseBody.countDown();
        }
    }

    @Test
    void signedCanaryPrecheckDeniedStillRecordsAnonymousObjectFacts() throws Exception {
        startServer(200, 206);
        S3AsyncClient s3 = provider(publicReadPolicy(), completedAcl());
        when(s3.headObject(any(Consumer.class))).thenReturn(CompletableFuture.failedFuture(
            S3Exception.builder().statusCode(403).build()));
        OssAccessDiagnostic result = client(s3).diagnoseAccess(
            "diagnostic/canary.txt", AccessPolicy.PUBLIC_READ, Duration.ofSeconds(2));
        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(observation(result, OssAccessDiagnostic.Subject.OBJECT_GET))
            .isEqualTo(OssAccessDiagnostic.Observation.ALLOWED);
    }

    @Test
    void interruptedSignedPrecheckStopsBeforeIndependentProviderProbes() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        when(s3.headObject(any(Consumer.class))).thenReturn(new CompletableFuture<>());
        OssAccessDiagnostic result;
        try {
            Thread.currentThread().interrupt();
            result = client(s3).diagnoseAccess(
                "diagnostic/canary.txt", AccessPolicy.PRIVATE, Duration.ofSeconds(2));
        } finally {
            Thread.interrupted();
        }
        assertThat(result.verification()).isEqualTo(OssAccessDiagnostic.Verification.UNVERIFIED);
        assertThat(result.facts()).allSatisfy(fact ->
            assertThat(fact.observation()).isEqualTo(OssAccessDiagnostic.Observation.UNKNOWN));
        verify(s3, never()).getBucketPolicy(any(Consumer.class));
        verify(s3, never()).getBucketAcl(any(Consumer.class));
    }

    private OssAccessDiagnostic.Observation observation(OssAccessDiagnostic result,
                                                        OssAccessDiagnostic.Subject subject) {
        return result.facts().stream().filter(fact -> fact.subject() == subject)
            .findFirst().orElseThrow().observation();
    }

    private S3AsyncClient unreadablePolicyAndAcl() {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        when(s3.headObject(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().contentLength(1L).build()));
        S3Exception denied = (S3Exception) S3Exception.builder().statusCode(403)
            .message("sensitive provider detail").build();
        when(s3.getBucketPolicy(any(Consumer.class))).thenReturn(CompletableFuture.failedFuture(denied));
        when(s3.getBucketAcl(any(Consumer.class))).thenReturn(CompletableFuture.failedFuture(denied));
        return s3;
    }

    private S3AsyncClient provider(String policy, CompletableFuture<GetBucketAclResponse> acl) {
        S3AsyncClient s3 = mock(S3AsyncClient.class);
        when(s3.headObject(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder().contentLength(1L).build()));
        if (policy == null) {
            S3Exception.Builder builder = S3Exception.builder();
            builder.statusCode(404);
            builder.message("NoSuchBucketPolicy");
            builder.awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchBucketPolicy").build());
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
        observedMethods.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bucket/diagnostic/canary.txt", exchange ->
            respond(exchange, "HEAD".equals(exchange.getRequestMethod()) ? headStatus : getStatus));
        server.start();
    }

    private void respond(HttpExchange exchange, int status) throws IOException {
        observedMethods.add(exchange.getRequestMethod());
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
