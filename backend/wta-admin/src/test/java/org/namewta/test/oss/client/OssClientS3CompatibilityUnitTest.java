package org.namewta.test.oss.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.model.OssCompletedPart;
import org.namewta.common.oss.model.OssObjectOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class OssClientS3CompatibilityUnitTest {

    @Test
    void realS3AsyncClientExecutesHeadMultipartAndCopyProtocol() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handleS3Request);
        server.start();
        try (OssClient client = new DefaultOssClientImpl("wire-test", clientConfig(server.getAddress().getPort()))) {
            assertEquals(12L, client.headObject("source.txt").size());

            String uploadId = client.createMultipartUpload("large.bin", OssObjectOptions.empty()).uploadId();
            assertEquals("upload-real", uploadId);
            assertEquals("part-real", client.listParts("large.bin", uploadId).getFirst().eTag());
            assertEquals(
                "complete-real",
                client.completeMultipartUpload(
                    "large.bin",
                    uploadId,
                    List.of(new OssCompletedPart(1, "part-real", Map.of()))
                ).eTag()
            );
            assertTrue(client.abortMultipartUpload("large.bin", uploadId));
            assertEquals("copy-real", client.copyObject("source.txt", "snapshot.txt").eTag());
        } finally {
            server.stop(0);
        }
    }

    private void handleS3Request(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getRawQuery();
        exchange.getRequestBody().readAllBytes();
        if ("HEAD".equals(method)) {
            exchange.getResponseHeaders().add("Content-Length", "12");
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.getResponseHeaders().add("ETag", "head-real");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        if ("POST".equals(method) && query != null && query.contains("uploads")) {
            respond(exchange, 200, """
                <InitiateMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <Bucket>media</Bucket><Key>large.bin</Key><UploadId>upload-real</UploadId>
                </InitiateMultipartUploadResult>
                """);
            return;
        }
        if ("GET".equals(method) && query != null && query.contains("uploadId=upload-real")) {
            respond(exchange, 200, """
                <ListPartsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <Bucket>media</Bucket><Key>large.bin</Key><UploadId>upload-real</UploadId>
                  <PartNumberMarker>0</PartNumberMarker><NextPartNumberMarker>1</NextPartNumberMarker>
                  <MaxParts>1000</MaxParts><IsTruncated>false</IsTruncated>
                  <Part><PartNumber>1</PartNumber><LastModified>2026-08-22T01:02:03.000Z</LastModified><ETag>part-real</ETag><Size>8</Size></Part>
                </ListPartsResult>
                """);
            return;
        }
        if ("POST".equals(method) && query != null && query.contains("uploadId=upload-real")) {
            respond(exchange, 200, """
                <CompleteMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <Location>http://localhost/media/large.bin</Location><Bucket>media</Bucket><Key>large.bin</Key><ETag>complete-real</ETag>
                </CompleteMultipartUploadResult>
                """);
            return;
        }
        if ("DELETE".equals(method) && query != null && query.contains("uploadId=upload-real")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if ("PUT".equals(method) && exchange.getRequestHeaders().containsKey("x-amz-copy-source")) {
            respond(exchange, 200, """
                <CopyObjectResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                  <LastModified>2026-08-22T01:02:03.000Z</LastModified><ETag>copy-real</ETag>
                </CopyObjectResult>
                """);
            return;
        }
        respond(exchange, 500, "<Error><Code>UnexpectedRequest</Code></Error>");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/xml");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private OssClientConfig clientConfig(int port) {
        return OssClientConfig.builder()
            .endpoint("127.0.0.1:" + port)
            .useHttps(false)
            .usePathStyleAccess(true)
            .accessKey("access-key")
            .secretKey("secret-key")
            .bucket("media")
            .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT)
            .build();
    }
}
