package org.namewta.test.oss.client;

import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.model.OssCompletedPart;
import org.namewta.common.oss.model.OssObjectOptions;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 MinIO 双 Bucket 上的浏览器直传、分片、访问边界和清理协议验证。
 */
@Tag("dev")
class MinioOssClientIntegrationTest {

    @Test
    void roundTripsTransfersAndEnforcesDualBucketBoundaries() throws Exception {
        String endpoint = System.getProperty("oss.minio.integration.endpoint");
        Assumptions.assumeTrue(endpoint != null && !endpoint.isBlank(), "需要一次性 MinIO endpoint");
        String accessKey = System.getProperty("oss.minio.integration.access-key", "namewta");
        String secretKey = System.getProperty("oss.minio.integration.secret-key", "namewta123");
        URI endpointUri = URI.create(endpoint);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String privateBucket = "namewta-ci-private-" + suffix;
        String publicBucket = "namewta-ci-public-" + suffix;
        String singleKey = "direct/single.txt";
        String multipartKey = "direct/multipart.bin";
        String publicKey = "direct/public.txt";
        byte[] singleBody = "namewta-minio-single".getBytes(StandardCharsets.UTF_8);
        byte[] multipartBody = "namewta-minio-multipart".getBytes(StandardCharsets.UTF_8);
        byte[] publicBody = "namewta-minio-public".getBytes(StandardCharsets.UTF_8);
        String publicPolicy = publicReadPolicy(publicBucket);

        try (S3Client bootstrap = bootstrap(endpointUri, accessKey, secretKey)) {
            bootstrap.createBucket(builder -> builder.bucket(privateBucket));
            bootstrap.createBucket(builder -> builder.bucket(publicBucket));
            bootstrap.putBucketPolicy(builder -> builder.bucket(publicBucket).policy(publicPolicy));
            String policyBefore = bootstrap.getBucketPolicy(builder -> builder.bucket(publicBucket)).policy();
            try (OssClient privateClient = new DefaultOssClientImpl(
                "minio-private-integration", clientConfig(endpointUri, accessKey, secretKey, privateBucket));
                 OssClient publicClient = new DefaultOssClientImpl(
                     "minio-public-integration", clientConfig(endpointUri, accessKey, secretKey, publicBucket))) {
                HttpClient http = HttpClient.newHttpClient();

                OssPresignedRequest singlePut = privateClient.presignPut(singleKey, Duration.ofMinutes(5),
                    new OssObjectOptions("text/plain", Map.of("test-owner", "namewta-ci"), null));
                assertEquals(200, executePut(http, singlePut, singleBody).statusCode());
                assertEquals(singleBody.length, privateClient.headObject(singleKey).size());

                HttpResponse<byte[]> singleGet = http.send(
                    HttpRequest.newBuilder(URI.create(privateClient.presignGet(singleKey, Duration.ofMinutes(5)).url()))
                        .GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, singleGet.statusCode());
                assertEquals("namewta-minio-single", new String(singleGet.body(), StandardCharsets.UTF_8));

                String uploadId = privateClient.createMultipartUpload(multipartKey, OssObjectOptions.empty()).uploadId();
                HttpResponse<byte[]> partResponse = executePut(http,
                    privateClient.presignUploadPart(multipartKey, uploadId, 1, Duration.ofMinutes(5)), multipartBody);
                assertEquals(200, partResponse.statusCode());
                String eTag = partResponse.headers().firstValue("etag").orElseThrow();
                assertEquals(multipartBody.length, privateClient.listParts(multipartKey, uploadId).getFirst().size());
                privateClient.completeMultipartUpload(multipartKey, uploadId,
                    java.util.List.of(new OssCompletedPart(1, eTag, Map.of())));
                assertEquals(multipartBody.length, privateClient.headObject(multipartKey).size());

                String abandoned = privateClient.createMultipartUpload("direct/abandoned.bin", OssObjectOptions.empty())
                    .uploadId();
                assertTrue(privateClient.abortMultipartUpload("direct/abandoned.bin", abandoned));

                assertEquals(403, anonymous(http, endpointUri, privateBucket, singleKey, "GET"));
                assertEquals(403, anonymous(http, endpointUri, privateBucket, singleKey, "HEAD"));
                assertEquals(403, anonymousPut(http, endpointUri, privateBucket));

                assertEquals(200, executePut(http,
                    publicClient.presignPut(publicKey, Duration.ofMinutes(5), OssObjectOptions.empty()),
                    publicBody).statusCode());
                assertEquals(200, anonymous(http, endpointUri, publicBucket, publicKey, "GET"));
                assertEquals(200, anonymous(http, endpointUri, publicBucket, publicKey, "HEAD"));
                assertEquals(403, anonymousPut(http, endpointUri, publicBucket));

                String expiringUrl = privateClient.presignGet(singleKey, Duration.ofSeconds(1)).url();
                assertEquals(200, http.send(HttpRequest.newBuilder(URI.create(expiringUrl)).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode());
                Thread.sleep(3100);
                assertEquals(403, http.send(HttpRequest.newBuilder(URI.create(expiringUrl)).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode());
                assertEquals(policyBefore,
                    bootstrap.getBucketPolicy(builder -> builder.bucket(publicBucket)).policy());
            } finally {
                bootstrap.deleteObject(builder -> builder.bucket(privateBucket).key(singleKey));
                bootstrap.deleteObject(builder -> builder.bucket(privateBucket).key(multipartKey));
                bootstrap.deleteObject(builder -> builder.bucket(publicBucket).key(publicKey));
                bootstrap.deleteBucketPolicy(builder -> builder.bucket(publicBucket));
                bootstrap.deleteBucket(builder -> builder.bucket(privateBucket));
                bootstrap.deleteBucket(builder -> builder.bucket(publicBucket));
            }
        }
    }

    private int anonymous(HttpClient http, URI endpoint, String bucket, String key, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "/" + bucket + "/" + key))
            .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private int anonymousPut(HttpClient http, URI endpoint, String bucket) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "/" + bucket + "/direct/anonymous.txt"))
            .PUT(HttpRequest.BodyPublishers.ofString("denied")).build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private HttpResponse<byte[]> executePut(HttpClient http, OssPresignedRequest request, byte[] body)
        throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url()))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        request.requiredHeaders().forEach(builder::header);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private S3Client bootstrap(URI endpoint, String accessKey, String secretKey) {
        return S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    private OssClientConfig clientConfig(URI endpoint, String accessKey, String secretKey, String bucket) {
        return OssClientConfig.builder()
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
    }

    private String publicReadPolicy(String bucket) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{"
            + "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},"
            + "\"Action\":[\"s3:GetObject\"],"
            + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }
}
