package org.namewta.test.oss.readiness;

import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssAccessDiagnostic;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 MinIO 双 Bucket 匿名访问边界与只读诊断验证。
 */
@Tag("dev")
class OssStorageReadinessMinioIntegrationTest {

    @Test
    void verifiesPublicAndPrivateBoundariesWithoutMutatingPolicy() throws Exception {
        String endpoint = System.getProperty("oss.minio.integration.endpoint");
        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(), "需要一次性 MinIO endpoint");
        String accessKey = System.getProperty("oss.minio.integration.access-key", "namewta");
        String secretKey = System.getProperty("oss.minio.integration.secret-key", "namewta123");
        URI endpointUri = URI.create(endpoint);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String publicBucket = "namewta-public-" + suffix;
        String privateBucket = "namewta-private-" + suffix;
        String canary = "diagnostic/canary.txt";
        byte[] body = "namewta-readiness-canary".getBytes(StandardCharsets.UTF_8);
        String publicPolicy = publicReadPolicy(publicBucket);

        try (S3Client bootstrap = bootstrap(endpointUri, accessKey, secretKey)) {
            bootstrap.createBucket(builder -> builder.bucket(publicBucket));
            bootstrap.createBucket(builder -> builder.bucket(privateBucket));
            try {
                bootstrap.putObject(builder -> builder.bucket(publicBucket).key(canary), RequestBody.fromBytes(body));
                bootstrap.putObject(builder -> builder.bucket(privateBucket).key(canary), RequestBody.fromBytes(body));
                bootstrap.putBucketPolicy(builder -> builder.bucket(publicBucket).policy(publicPolicy));
                String policyBefore = bootstrap.getBucketPolicy(builder -> builder.bucket(publicBucket)).policy();

                try (OssClient publicClient = client(endpointUri, accessKey, secretKey, publicBucket);
                     OssClient privateClient = client(endpointUri, accessKey, secretKey, privateBucket)) {
                    OssAccessDiagnostic publicResult = publicClient.diagnoseAccess(
                        canary, AccessPolicy.PUBLIC_READ, Duration.ofSeconds(3));
                    OssAccessDiagnostic privateResult = privateClient.diagnoseAccess(
                        canary, AccessPolicy.PRIVATE, Duration.ofSeconds(3));

                    assertThat(publicResult.verified()).isTrue();
                    assertThat(publicResult.anonymousHeadAllowed()).isTrue();
                    assertThat(publicResult.anonymousGetAllowed()).isTrue();
                    assertThat(publicResult.anonymousWriteDenied()).isTrue();
                    assertThat(privateResult.verified()).isTrue();
                    assertThat(privateResult.anonymousHeadAllowed()).isFalse();
                    assertThat(privateResult.anonymousGetAllowed()).isFalse();
                    assertThat(privateResult.anonymousWriteDenied()).isTrue();
                }

                HttpClient http = HttpClient.newHttpClient();
                assertThat(status(http, endpointUri, publicBucket, canary, "HEAD")).isEqualTo(200);
                assertThat(status(http, endpointUri, publicBucket, canary, "GET")).isIn(200, 206);
                assertThat(status(http, endpointUri, privateBucket, canary, "HEAD")).isEqualTo(403);
                assertThat(status(http, endpointUri, privateBucket, canary, "GET")).isEqualTo(403);
                assertThat(anonymousPut(http, endpointUri, publicBucket)).isEqualTo(403);
                assertThat(anonymousPut(http, endpointUri, privateBucket)).isEqualTo(403);
                assertThat(bootstrap.getBucketPolicy(builder -> builder.bucket(publicBucket)).policy())
                    .isEqualTo(policyBefore);
            } finally {
                bootstrap.deleteObject(builder -> builder.bucket(publicBucket).key(canary));
                bootstrap.deleteObject(builder -> builder.bucket(privateBucket).key(canary));
                bootstrap.deleteBucketPolicy(builder -> builder.bucket(publicBucket));
                bootstrap.deleteBucket(builder -> builder.bucket(publicBucket));
                bootstrap.deleteBucket(builder -> builder.bucket(privateBucket));
            }
        }
    }

    private int status(HttpClient http, URI endpoint, String bucket, String key, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(objectUri(endpoint, bucket, key))
            .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private int anonymousPut(HttpClient http, URI endpoint, String bucket) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(objectUri(endpoint, bucket, "diagnostic/anonymous-write.txt"))
            .PUT(HttpRequest.BodyPublishers.ofString("denied")).build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private URI objectUri(URI endpoint, String bucket, String key) {
        return URI.create(endpoint.toString() + "/" + bucket + "/" + key);
    }

    private S3Client bootstrap(URI endpoint, String accessKey, String secretKey) {
        return S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    private OssClient client(URI endpoint, String accessKey, String secretKey, String bucket) {
        OssClientConfig config = OssClientConfig.builder()
            .endpoint(endpoint.getAuthority())
            .useHttps("https".equalsIgnoreCase(endpoint.getScheme()))
            .usePathStyleAccess(true)
            .accessKey(accessKey)
            .secretKey(secretKey)
            .bucket(bucket)
            .region(Region.US_EAST_1)
            .prefix("")
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT)
            .build();
        return new DefaultOssClientImpl("readiness-" + bucket, config);
    }

    private String publicReadPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{"
            + "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},"
            + "\"Action\":[\"s3:GetObject\"],"
            + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }
}
