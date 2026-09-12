package org.namewta.test.oss.access;

import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.AccessControlPolicyConfig;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.namewta.system.oss.mapper.SysOssRefMapper;
import org.namewta.system.oss.provider.DefaultOssObjectStore;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Tag("dev")
class OssAccessUrlMinioIntegrationTest {

    @Test
    void resolvesStablePublicAndExpiringPrivateUrlsAgainstRealMinio() throws Exception {
        String endpoint = System.getProperty("oss.minio.integration.endpoint");
        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(), "需要一次性 MinIO endpoint");
        String accessKey = System.getProperty("oss.minio.integration.access-key", "namewta");
        String secretKey = System.getProperty("oss.minio.integration.secret-key", "namewta123");
        URI endpointUri = URI.create(endpoint);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String publicBucket = "namewta-access-public-" + suffix;
        String privateBucket = "namewta-access-private-" + suffix;
        String key = "documents/a b.txt";
        byte[] body = "namewta-access-url".getBytes(StandardCharsets.UTF_8);

        try (S3Client bootstrap = bootstrap(endpointUri, accessKey, secretKey)) {
            bootstrap.createBucket(builder -> builder.bucket(publicBucket));
            bootstrap.createBucket(builder -> builder.bucket(privateBucket));
            try {
                bootstrap.putObject(builder -> builder.bucket(publicBucket).key(key), RequestBody.fromBytes(body));
                bootstrap.putObject(builder -> builder.bucket(privateBucket).key(key), RequestBody.fromBytes(body));
                bootstrap.putBucketPolicy(builder -> builder.bucket(publicBucket)
                    .policy(publicReadPolicy(publicBucket)));

                try (OssClient publicClient = client(endpointUri, accessKey, secretKey, publicBucket,
                    endpointUri.getAuthority() + "/" + publicBucket, AccessPolicy.PUBLIC_READ);
                     OssClient privateClient = client(endpointUri, accessKey, secretKey, privateBucket,
                         null, AccessPolicy.PRIVATE);
                     MockedStatic<OssFactory> factory = mockStatic(OssFactory.class)) {
                    factory.when(() -> OssFactory.instance("public")).thenReturn(publicClient);
                    factory.when(() -> OssFactory.instance("private")).thenReturn(privateClient);
                    OssLifecycleManager manager = manager(key);
                    HttpClient http = HttpClient.newHttpClient();

                    OssService.OssAccessUrl publicUrl = manager.resolveAccessUrl(1L);
                    assertThat(publicUrl.accessType()).isEqualTo("PUBLIC");
                    assertThat(publicUrl.expiresAt()).isNull();
                    assertThat(publicUrl.url()).doesNotContain("?", "X-Amz-");
                    assertThat(publicUrl.url()).contains("a%20b.txt");
                    assertThat(get(http, publicUrl.url())).isEqualTo(200);
                    assertThat(head(http, publicUrl.url())).isEqualTo(200);
                    assertThatThrownBy(() -> manager.presignDownload(1L))
                        .isInstanceOfSatisfying(OssLifecycleException.class,
                            ex -> assertThat(ex.error()).isEqualTo(OssLifecycleError.PUBLIC_PRESIGN_FORBIDDEN));

                    OssService.OssAccessUrl privateUrl = manager.resolveAccessUrl(2L);
                    assertThat(privateUrl.accessType()).isEqualTo("PRIVATE");
                    assertThat(privateUrl.expiresAt()).isAfter(Instant.now());
                    assertThat(privateUrl.url()).contains("X-Amz-");
                    assertThat(get(http, rawUrl(endpointUri, privateBucket, key))).isEqualTo(403);
                    assertThat(get(http, privateUrl.url())).isEqualTo(200);

                    Thread.sleep(3100);
                    assertThat(get(http, privateUrl.url())).isEqualTo(403);
                    OssService.OssAccessUrl renewed = manager.resolveAccessUrl(2L);
                    assertThat(renewed.url()).isNotEqualTo(privateUrl.url());
                    assertThat(get(http, renewed.url())).isEqualTo(200);
                }
            } finally {
                bootstrap.deleteObject(builder -> builder.bucket(publicBucket).key(key));
                bootstrap.deleteObject(builder -> builder.bucket(privateBucket).key(key));
                bootstrap.deleteBucketPolicy(builder -> builder.bucket(publicBucket));
                bootstrap.deleteBucket(builder -> builder.bucket(publicBucket));
                bootstrap.deleteBucket(builder -> builder.bucket(privateBucket));
            }
        }
    }

    private OssLifecycleManager manager(String key) {
        SysOss publicOss = object(1L, "public", key);
        SysOss privateOss = object(2L, "private", key);
        SysOssMapper mapper = mock(SysOssMapper.class);
        when(mapper.selectById(1L)).thenReturn(publicOss);
        when(mapper.selectById(2L)).thenReturn(privateOss);
        OssLifecycleProperties lifecycleProperties = new OssLifecycleProperties();
        lifecycleProperties.setDownloadTtlMin(Duration.ofSeconds(1));
        lifecycleProperties.setDownloadTtl(Duration.ofSeconds(2));
        lifecycleProperties.validate();
        OssStorageReadinessProperties readinessProperties = new OssStorageReadinessProperties();
        readinessProperties.setMaxSnapshotAge(Duration.ofMinutes(10));
        OssStorageReadinessRegistry registry = new OssStorageReadinessRegistry(readinessProperties);
        Instant now = Instant.now();
        registry.replace(Map.of(
            "public", serving("public", AccessPolicy.PUBLIC_READ, now),
            "private", serving("private", AccessPolicy.PRIVATE, now)
        ), Set.of("public", "private"), true);
        return new OssLifecycleManager(mapper, mock(SysOssRefMapper.class),
            new DefaultOssObjectStore(), lifecycleProperties, registry);
    }

    private OssStorageReadinessEntry serving(String key, AccessPolicy policy, Instant now) {
        return new OssStorageReadinessEntry(key, policy, true, Set.of("E2E"),
            OssStorageReadinessEntry.Status.SERVING, OssStorageReadinessEntry.Reason.READY, now);
    }

    private SysOss object(Long id, String service, String key) {
        SysOss oss = new SysOss();
        oss.setOssId(id);
        oss.setService(service);
        oss.setFileName(key);
        oss.setOriginalName("a b.txt");
        oss.setDeleteState("ACTIVE");
        return oss;
    }

    private int get(HttpClient http, String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private int head(HttpClient http, String url) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(url))
            .method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private String rawUrl(URI endpoint, String bucket, String key) {
        return endpoint + "/" + bucket + "/" + SdkHttpUtils.urlEncodeIgnoreSlashes(key);
    }

    private S3Client bootstrap(URI endpoint, String accessKey, String secretKey) {
        return S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    private OssClient client(URI endpoint, String accessKey, String secretKey, String bucket,
                             String domain, AccessPolicy policy) {
        OssClientConfig config = OssClientConfig.builder()
            .endpoint(endpoint.getAuthority())
            .domain(domain)
            .useHttps(false)
            .usePathStyleAccess(true)
            .accessKey(accessKey)
            .secretKey(secretKey)
            .bucket(bucket)
            .region(Region.US_EAST_1)
            .prefix("")
            .accessControlPolicyConfig(AccessControlPolicyConfig.builder()
                .enabled(true).accessPolicy(policy).build())
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT)
            .build();
        return new DefaultOssClientImpl("access-" + bucket, config);
    }

    private String publicReadPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{"
            + "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},"
            + "\"Action\":[\"s3:GetObject\"],"
            + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }
}
