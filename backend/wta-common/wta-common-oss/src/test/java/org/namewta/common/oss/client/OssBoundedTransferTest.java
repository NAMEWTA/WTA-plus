package org.namewta.common.oss.client;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.exception.S3StorageException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** 只替换 SDK 网络边界，验证有界消费与订阅取消，不依赖供应商服务。 */
@Tag("dev")
class OssBoundedTransferTest {

    @Test
    void boundedDownloadRejectsOversizeAndCancelsPublisher() throws Exception {
        AtomicBoolean cancelled = new AtomicBoolean();
        try (DefaultOssClientImpl client = client()) {
            S3AsyncClient original = client.s3AsyncClient;
            client.s3AsyncClient = fake(publisher(new byte[] {1, 2, 3, 4}, cancelled));
            try {
                assertThrows(S3StorageException.class,
                    () -> client.downloadBounded("item", 3, Duration.ofSeconds(1)));
                assertTrue(cancelled.get(), "oversize must cancel the actual Subscription");
            } finally {
                client.s3AsyncClient = original;
            }
        }
    }

    @Test
    void stalledPublisherIsCancelledAtDeadlineWithoutWaitingForAnotherChunk() throws Exception {
        AtomicBoolean cancelled = new AtomicBoolean();
        try (DefaultOssClientImpl client = client()) {
            S3AsyncClient original = client.s3AsyncClient;
            client.s3AsyncClient = fake(publisher(null, cancelled));
            try {
                long started = System.nanoTime();
                assertThrows(S3StorageException.class,
                    () -> client.downloadBounded("stalled", 1024, Duration.ofMillis(100)));
                assertTrue(cancelled.get(), "timeout must cancel Subscription, not only its completion future");
                assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(2)) < 0);
            } finally {
                client.s3AsyncClient = original;
            }
        }
    }

    @Test
    void boundedReadAndPutReturnExactBytesAndSize() throws Exception {
        AtomicBoolean cancelled = new AtomicBoolean();
        try (DefaultOssClientImpl client = client()) {
            S3AsyncClient original = client.s3AsyncClient;
            client.s3AsyncClient = fake(publisher(new byte[] {5, 6, 7}, cancelled));
            try {
                assertArrayEquals(new byte[] {5, 6, 7},
                    client.downloadBounded("item", 3, Duration.ofSeconds(1)));
                assertFalse(cancelled.get());
                assertEquals(3, client.uploadBounded("copy", new byte[] {5, 6, 7},
                    "application/octet-stream", Duration.ofSeconds(1)).size());
            } finally {
                client.s3AsyncClient = original;
            }
        }
    }

    private DefaultOssClientImpl client() {
        return new DefaultOssClientImpl("bounded-test", OssClientConfig.builder()
            .endpoint("127.0.0.1:1").useHttps(false).usePathStyleAccess(true)
            .accessKey("owned-access").secretKey("owned-secret").bucket("owned-bucket")
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT).build());
    }

    private S3AsyncClient fake(ResponsePublisher<GetObjectResponse> publisher) {
        return (S3AsyncClient) Proxy.newProxyInstance(S3AsyncClient.class.getClassLoader(),
            new Class<?>[] {S3AsyncClient.class}, (proxy, method, args) -> {
                if ("getObject".equals(method.getName())) return CompletableFuture.completedFuture(publisher);
                if ("putObject".equals(method.getName())) return CompletableFuture.completedFuture(
                    PutObjectResponse.builder().eTag("owned-etag").build());
                if ("close".equals(method.getName())) return null;
                throw new UnsupportedOperationException(method.getName());
            });
    }

    private ResponsePublisher<GetObjectResponse> publisher(byte[] bytes, AtomicBoolean cancelled) {
        SdkPublisher<ByteBuffer> body = new SdkPublisher<>() {
            @Override public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    private boolean sent;
                    @Override public void request(long count) {
                        if (bytes == null || sent || cancelled.get()) return;
                        sent = true;
                        subscriber.onNext(ByteBuffer.wrap(bytes));
                        if (!cancelled.get()) subscriber.onComplete();
                    }
                    @Override public void cancel() { cancelled.set(true); }
                });
            }
        };
        return new ResponsePublisher<>(GetObjectResponse.builder().contentLength(bytes == null ? 1L : (long) bytes.length)
            .build(), body);
    }
}
