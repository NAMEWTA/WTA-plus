package org.namewta.test.oss.upload;

import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.AccessControlPolicyConfig;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.system.oss.upload.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.namewta.system.oss.upload.OssUploadContracts.*;
import static org.mockito.Mockito.mockStatic;

/**
 * 真实双 Bucket 上传路由验证。
 */
@Tag("dev")
class OssUploadStorageRoutingMinioIntegrationTest {

    @Test
    void routesSingleAndMultipartUploadsToPolicyBoundBuckets() throws Exception {
        String endpoint = System.getProperty("oss.minio.integration.endpoint");
        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(), "需要一次性 MinIO endpoint");
        String accessKey = System.getProperty("oss.minio.integration.access-key", "namewta");
        String secretKey = System.getProperty("oss.minio.integration.secret-key", "namewta123");
        URI endpointUri = URI.create(endpoint);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String publicBucket = "namewta-upload-public-" + suffix;
        String privateBucket = "namewta-upload-private-" + suffix;
        String[] uploadedKeys = new String[2];

        try (S3Client bootstrap = bootstrap(endpointUri, accessKey, secretKey)) {
            bootstrap.createBucket(builder -> builder.bucket(publicBucket));
            bootstrap.createBucket(builder -> builder.bucket(privateBucket));
            bootstrap.putBucketPolicy(builder -> builder.bucket(publicBucket).policy(publicReadPolicy(publicBucket)));
            try (OssClient publicClient = client("public-route", endpointUri, accessKey, secretKey,
                publicBucket, AccessPolicy.PUBLIC_READ);
                 OssClient privateClient = client("private-route", endpointUri, accessKey, secretKey,
                     privateBucket, AccessPolicy.PRIVATE);
                 MockedStatic<OssFactory> factory = mockStatic(OssFactory.class)) {
                factory.when(() -> OssFactory.instance("public-route")).thenReturn(publicClient);
                factory.when(() -> OssFactory.instance("private-route")).thenReturn(privateClient);

                MemoryTicketStore tickets = new MemoryTicketStore();
                MemoryMetadataStore metadata = new MemoryMetadataStore();
                AtomicReference<String> defaultKey = new AtomicReference<>("private-route");
                OssUploadService service = new OssUploadService(properties(), identity(), tickets,
                    new DefaultOssUploadObjectStore(), metadata, readiness(), defaultKey::get);
                HttpClient http = HttpClient.newHttpClient();

                byte[] privateBody = "private-multipart".getBytes(StandardCharsets.UTF_8);
                InitResponse privateInit = service.init(new InitRequest("attachment", "private.bin",
                    privateBody.length, "application/octet-stream", "private-fingerprint"));
                OssUploadTicket privateTicket = tickets.get(privateInit.uploadToken());
                uploadedKeys[1] = privateTicket.objectKey();
                assertThat(privateInit.mode()).isEqualTo(OssUploadMode.MULTIPART);
                assertThat(privateTicket.service()).isEqualTo("private-route");
                assertThat(privateTicket.bucket()).isEqualTo(privateBucket);
                SignedPart part = service.signParts(privateInit.uploadToken(),
                    new SignPartsRequest(List.of(1))).parts().getFirst();
                HttpResponse<byte[]> partResponse = put(http, part, privateBody);
                assertThat(partResponse.statusCode()).isEqualTo(200);
                String eTag = partResponse.headers().firstValue("etag").orElseThrow();
                service.complete(privateInit.uploadToken(),
                    new CompleteRequest(List.of(new CompletedPart(1, eTag))));
                assertThat(metadata.registered.get(privateInit.uploadToken()).service()).isEqualTo("private-route");
                assertThat(privateClient.headObject(uploadedKeys[1]).size()).isEqualTo(privateBody.length);
                assertThat(rawGet(http, endpointUri, privateBucket, uploadedKeys[1])).isEqualTo(403);

                defaultKey.set("public-route");
                assertThatThrownBy(() -> service.init(new InitRequest("portal", "portal.txt",
                    4, "application/octet-stream", "public-fingerprint")))
                    .isInstanceOf(OssUploadException.class)
                    .extracting(error -> ((OssUploadException) error).error())
                    .isEqualTo(OssUploadError.STORAGE_ACCESS_POLICY_MISMATCH);
            } finally {
                if (uploadedKeys[0] != null) {
                    bootstrap.deleteObject(builder -> builder.bucket(publicBucket).key(uploadedKeys[0]));
                }
                if (uploadedKeys[1] != null) {
                    bootstrap.deleteObject(builder -> builder.bucket(privateBucket).key(uploadedKeys[1]));
                }
                bootstrap.deleteBucketPolicy(builder -> builder.bucket(publicBucket));
                bootstrap.deleteBucket(builder -> builder.bucket(publicBucket));
                bootstrap.deleteBucket(builder -> builder.bucket(privateBucket));
            }
        }
    }

    private OssUploadProperties properties() {
        OssUploadProperties.Policy portal = policy(AccessPolicy.PRIVATE, "direct/portal", OssUploadMode.SINGLE);
        OssUploadProperties.Policy attachment = policy(AccessPolicy.PRIVATE, "direct/private", OssUploadMode.MULTIPART);
        OssUploadProperties properties = new OssUploadProperties();
        properties.setPolicies(Map.of("portal", portal, "attachment", attachment));
        properties.validate();
        return properties;
    }

    private OssUploadProperties.Policy policy(AccessPolicy accessPolicy, String prefix, OssUploadMode mode) {
        OssUploadProperties.Policy policy = new OssUploadProperties.Policy();
        policy.setExpectedAccessPolicy(accessPolicy);
        policy.setMaxSize(10L * 1024 * 1024);
        policy.setAllowedContentTypes(Set.of("application/octet-stream"));
        policy.setObjectPrefix(prefix);
        policy.setMode(mode);
        policy.setMultipartThreshold(1);
        policy.setPartSize(OssUploadProperties.MIN_PART_SIZE);
        return policy;
    }

    private OssStorageReadinessRegistry readiness() {
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        properties.setMaxSnapshotAge("PT10M");
        OssStorageReadinessRegistry registry = new OssStorageReadinessRegistry(properties);
        Instant now = Instant.now();
        registry.replace(Map.of(
            "public-route", serving("public-route", AccessPolicy.PUBLIC_READ, now),
            "private-route", serving("private-route", AccessPolicy.PRIVATE, now)
        ), Set.of("public-route", "private-route"), true);
        return registry;
    }

    private OssStorageReadinessEntry serving(String key, AccessPolicy policy, Instant now) {
        return new OssStorageReadinessEntry(key, policy, true, Set.of("UPLOAD_POLICY"),
            OssStorageReadinessEntry.Status.SERVING, OssStorageReadinessEntry.Reason.READY, now);
    }

    private OssUploadIdentityResolver identity() {
        return new OssUploadIdentityResolver() {
            @Override
            public Identity resolve() {
                return new Identity(7L, 100L);
            }

            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        };
    }

    private HttpResponse<byte[]> put(HttpClient http, OssPresignedRequest request, byte[] body) throws Exception {
        return put(http, request.url(), request.requiredHeaders(), body);
    }

    private HttpResponse<byte[]> put(HttpClient http, SignedPart request, byte[] body) throws Exception {
        return put(http, request.url(), request.requiredHeaders(), body);
    }

    private HttpResponse<byte[]> put(HttpClient http, String url, Map<String, String> headers, byte[] body)
        throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        headers.forEach(builder::header);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private int rawGet(HttpClient http, URI endpoint, String bucket, String key) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(endpoint + "/" + bucket + "/" + key)).GET().build(),
            HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private void assertThatThrownByHead(OssClient client, String key) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.headObject(key));
    }

    private S3Client bootstrap(URI endpoint, String accessKey, String secretKey) {
        return S3Client.builder().endpointOverride(endpoint).region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
    }

    private OssClient client(String configKey, URI endpoint, String accessKey, String secretKey, String bucket,
                             AccessPolicy policy) {
        OssClientConfig config = OssClientConfig.builder()
            .endpoint(endpoint.getAuthority()).useHttps(false).usePathStyleAccess(true)
            .accessKey(accessKey).secretKey(secretKey).bucket(bucket).region(Region.US_EAST_1).prefix("")
            .accessControlPolicyConfig(AccessControlPolicyConfig.builder()
                .enabled(true).accessPolicy(policy).build())
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT).build();
        return new DefaultOssClientImpl(configKey, config);
    }

    private String publicReadPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{"
            + "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},"
            + "\"Action\":[\"s3:GetObject\"],"
            + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }

    private static final class MemoryTicketStore implements OssUploadTicketStore {
        private final Map<String, OssUploadTicket> tickets = new ConcurrentHashMap<>();
        private final Map<String, OssUploadCleanupRecord> cleanup = new ConcurrentHashMap<>();

        @Override
        public void create(OssUploadTicket ticket, OssUploadCleanupRecord record, Duration ticketTtl,
                           Duration cleanupTtl) {
            tickets.put(ticket.token(), ticket);
            cleanup.put(ticket.token(), record);
        }

        @Override public OssUploadTicket get(String token) { return tickets.get(token); }
        @Override public void save(OssUploadTicket ticket, Duration ttl) { tickets.put(ticket.token(), ticket); }
        @Override public void removeCompletedCleanup(String token) { cleanup.remove(token); }
        @Override public void removeSession(String token) { tickets.remove(token); cleanup.remove(token); }
        @Override public List<String> findExpired(long now, int limit) { return List.of(); }
        @Override public OssUploadCleanupRecord getCleanup(String token) { return cleanup.get(token); }
        @Override public void scheduleCleanup(String token, long when) { }
        @Override public synchronized <T> T locked(String token, Supplier<T> action) { return action.get(); }
    }

    private static final class MemoryMetadataStore implements OssUploadMetadataStore {
        private final AtomicLong ids = new AtomicLong(1000);
        private final Map<String, Long> byObject = new ConcurrentHashMap<>();
        private final Map<String, OssUploadTicket> registered = new ConcurrentHashMap<>();

        @Override
        public Long findByObject(String service, String objectKey) {
            return byObject.get(service + "\n" + objectKey);
        }

        @Override
        public Long registerTemporary(OssUploadTicket ticket) {
            long id = ids.incrementAndGet();
            byObject.put(ticket.service() + "\n" + ticket.objectKey(), id);
            registered.put(ticket.token(), ticket);
            return id;
        }
    }
}
