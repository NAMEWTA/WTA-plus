package org.namewta.common.oss.client;

import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.exception.OssErrorCode;
import org.namewta.common.oss.exception.S3StorageException;
import org.namewta.common.oss.model.OssChecksumAlgorithm;
import org.namewta.common.oss.model.OssCompletedPart;
import org.namewta.common.oss.model.OssCopyResult;
import org.namewta.common.oss.model.OssMultipartCompleteResult;
import org.namewta.common.oss.model.OssMultipartPart;
import org.namewta.common.oss.model.OssMultipartUpload;
import org.namewta.common.oss.model.OssObjectOptions;
import org.namewta.common.oss.model.OssObjectStat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class OssClientProviderUnitTest {

    @Test
    void providerOperationsReturnProjectOwnedModels() throws Exception {
        Instant modifiedAt = Instant.parse("2026-08-22T01:02:03Z");
        try (TestOssClient client = new TestOssClient(clientConfig())) {
            S3AsyncClient provider = client.provider();
            when(provider.headObject(any(HeadObjectRequest.class))).thenReturn(CompletableFuture.completedFuture(
                HeadObjectResponse.builder()
                    .contentLength(12L)
                    .contentType("text/plain")
                    .eTag("head-etag")
                    .lastModified(modifiedAt)
                    .metadata(Map.of("origin", "upload"))
                    .build()
            ));
            when(provider.createMultipartUpload(any(CreateMultipartUploadRequest.class))).thenReturn(CompletableFuture.completedFuture(
                CreateMultipartUploadResponse.builder().bucket("media").key("large.bin").uploadId("upload-1").build()
            ));
            when(provider.listParts(any(ListPartsRequest.class))).thenReturn(CompletableFuture.completedFuture(
                ListPartsResponse.builder()
                    .bucket("media")
                    .key("large.bin")
                    .uploadId("upload-1")
                    .isTruncated(true)
                    .nextPartNumberMarker(1)
                    .parts(Part.builder().partNumber(1).eTag("part-etag").size(8L).lastModified(modifiedAt).build())
                    .build()
            ), CompletableFuture.completedFuture(
                ListPartsResponse.builder()
                    .bucket("media")
                    .key("large.bin")
                    .uploadId("upload-1")
                    .isTruncated(false)
                    .parts(Part.builder().partNumber(2).eTag("part-etag-2").size(4L).lastModified(modifiedAt).build())
                    .build()
            ));
            when(provider.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenReturn(CompletableFuture.completedFuture(
                CompleteMultipartUploadResponse.builder().bucket("media").key("large.bin").eTag("multipart-etag-2").build()
            ));
            when(provider.abortMultipartUpload(any(AbortMultipartUploadRequest.class))).thenReturn(CompletableFuture.completedFuture(
                AbortMultipartUploadResponse.builder().build()
            ));
            when(provider.copyObject(any(CopyObjectRequest.class))).thenReturn(CompletableFuture.completedFuture(
                CopyObjectResponse.builder()
                    .copyObjectResult(builder -> builder.eTag("copy-etag").lastModified(modifiedAt))
                    .build()
            ));

            OssObjectStat stat = client.headObject("source folder/a.txt");
            OssMultipartUpload upload = client.createMultipartUpload("large.bin", OssObjectOptions.empty());
            List<OssMultipartPart> parts = client.listParts("large.bin", "upload-1");
            OssMultipartCompleteResult completed = client.completeMultipartUpload(
                "large.bin",
                "upload-1",
                List.of(new OssCompletedPart(1, "part-etag", Map.of()))
            );
            boolean aborted = client.abortMultipartUpload("large.bin", "upload-1");
            OssCopyResult copied = client.copyObject("source folder/a.txt", "snapshots/a.txt");

            assertEquals(12L, stat.size());
            assertEquals("upload-1", upload.uploadId());
            assertEquals("part-etag", parts.getFirst().eTag());
            assertEquals("part-etag-2", parts.getLast().eTag());
            assertEquals("multipart-etag-2", completed.eTag());
            assertTrue(aborted);
            assertEquals("copy-etag", copied.eTag());

            ArgumentCaptor<CompleteMultipartUploadRequest> completeCaptor = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
            verify(provider).completeMultipartUpload(completeCaptor.capture());
            assertEquals("part-etag", completeCaptor.getValue().multipartUpload().parts().getFirst().eTag());

            ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
            verify(provider).copyObject(copyCaptor.capture());
            assertEquals("media/source%20folder/a.txt", copyCaptor.getValue().copySource());

            ArgumentCaptor<AbortMultipartUploadRequest> abortCaptor = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
            verify(provider).abortMultipartUpload(abortCaptor.capture());
            assertEquals("upload-1", abortCaptor.getValue().uploadId());

            ArgumentCaptor<ListPartsRequest> listCaptor = ArgumentCaptor.forClass(ListPartsRequest.class);
            verify(provider, org.mockito.Mockito.times(2)).listParts(listCaptor.capture());
            assertEquals(1, listCaptor.getAllValues().getLast().partNumberMarker());
        }
    }

    @Test
    void providerFailuresAreCategorizedWithoutLeakingSignedMaterial() throws Exception {
        try (TestOssClient client = new TestOssClient(clientConfig())) {
            String sensitiveMessage = "denied https://oss.example/a?X-Amz-Signature=token secret-key";
            S3Exception providerFailure = (S3Exception) S3Exception.builder()
                .statusCode(403)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").errorMessage(sensitiveMessage).build())
                .message(sensitiveMessage)
                .build();
            when(client.provider().headObject(any(HeadObjectRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(providerFailure));

            S3StorageException error = assertThrows(S3StorageException.class, () -> client.headObject("private.txt"));

            assertEquals(OssErrorCode.PROVIDER_ERROR, error.code());
            assertTrue(error.getMessage().contains("AccessDenied"));
            assertFalse(error.getMessage().contains("X-Amz-Signature"));
            assertFalse(error.getMessage().contains("secret-key"));
        }
    }

    @Test
    void unsupportedChecksumAndMissingAbortHaveStableBehavior() throws Exception {
        try (TestOssClient client = new TestOssClient(clientConfig())) {
            OssObjectOptions unsupported = new OssObjectOptions("application/octet-stream", Map.of(), OssChecksumAlgorithm.SHA256);

            S3StorageException error = assertThrows(
                S3StorageException.class,
                () -> client.createMultipartUpload("large.bin", unsupported)
            );
            assertEquals(OssErrorCode.UNSUPPORTED_CAPABILITY, error.code());

            S3Exception missing = (S3Exception) S3Exception.builder().statusCode(404).message("missing").build();
            when(client.provider().abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(missing));
            assertTrue(client.abortMultipartUpload("large.bin", "upload-missing"));
        }
    }

    @Test
    void invalidIdentityAndMissingObjectUseStableErrorCategories() throws Exception {
        try (TestOssClient client = new TestOssClient(clientConfig())) {
            S3StorageException invalid = assertThrows(
                S3StorageException.class,
                () -> client.headObject(" ")
            );
            assertEquals(OssErrorCode.INVALID_REQUEST, invalid.code());

            S3Exception missing = (S3Exception) S3Exception.builder().statusCode(404).message("missing").build();
            when(client.provider().headObject(any(HeadObjectRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(missing));

            S3StorageException notFound = assertThrows(
                S3StorageException.class,
                () -> client.headObject("missing.txt")
            );
            assertEquals(OssErrorCode.OBJECT_NOT_FOUND, notFound.code());
        }
    }

    @Test
    void boundedMigrationHeadAndDeleteCancelTheirOwnUnfinishedRequests() throws Exception {
        try (TestOssClient client = new TestOssClient(clientConfig())) {
            CompletableFuture<HeadObjectResponse> hangingHead = new CompletableFuture<>();
            CompletableFuture<DeleteObjectResponse> hangingDelete = new CompletableFuture<>();
            when(client.provider().headObject(any(HeadObjectRequest.class))).thenReturn(hangingHead);
            when(client.provider().deleteObject(any(DeleteObjectRequest.class))).thenReturn(hangingDelete);

            assertThrows(S3StorageException.class,
                () -> client.headObject("source.txt", Duration.ofMillis(30)));
            assertTrue(hangingHead.isCancelled());
            assertThrows(S3StorageException.class,
                () -> client.delete("source.txt", Duration.ofMillis(30)));
            assertTrue(hangingDelete.isCancelled());
            verify(client.provider()).headObject(any(HeadObjectRequest.class));
            verify(client.provider()).deleteObject(any(DeleteObjectRequest.class));
        }
    }

    private OssClientConfig clientConfig() {
        return OssClientConfig.builder()
            .endpoint("localhost:9000")
            .useHttps(false)
            .usePathStyleAccess(true)
            .accessKey("access-key")
            .secretKey("secret-key")
            .bucket("media")
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT)
            .build();
    }

    private static final class TestOssClient extends AbstractOssClientImpl {

        private TestOssClient(OssClientConfig config) {
            super("test", config);
        }

        @Override
        void doInitialize() {
            s3AsyncClient = mock(S3AsyncClient.class);
        }

        private S3AsyncClient provider() {
            return s3AsyncClient;
        }
    }
}
