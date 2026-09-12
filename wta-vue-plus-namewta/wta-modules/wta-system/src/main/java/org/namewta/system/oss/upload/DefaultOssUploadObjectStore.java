package org.namewta.system.oss.upload;

import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.exception.OssErrorCode;
import org.namewta.common.oss.exception.S3StorageException;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.common.oss.model.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * common-oss 的上传控制面适配器。
 */
@Component
public class DefaultOssUploadObjectStore implements OssUploadObjectStore {

    @Override
    public PreparedUpload prepare(String storageConfigKey, AccessPolicy expectedAccessPolicy, String objectPrefix,
                                  String fileName, String contentType, String fingerprintDigest,
                                  OssUploadMode mode, Duration presignTtl) {
        OssClient client = OssFactory.instance(storageConfigKey);
        AccessPolicy actualAccessPolicy = client.config().accessControlPolicyConfig().accessPolicy();
        if (actualAccessPolicy != expectedAccessPolicy) {
            throw new OssUploadException(OssUploadError.STORAGE_ACCESS_POLICY_MISMATCH,
                "OSS 上传目标访问策略与服务端策略不一致");
        }
        if (mode == OssUploadMode.MULTIPART && !client.capabilities().multipartUpload()) {
            throw new OssUploadException(OssUploadError.INVALID_POLICY, "当前 OSS Provider 不支持 Multipart");
        }
        String key = client.buildPathKey(objectPrefix, fileName);
        String bucket = client.config().bucket().orElseThrow(() ->
            new OssUploadException(OssUploadError.INVALID_POLICY, "OSS Provider 未配置 Bucket"));
        OssObjectOptions options = new OssObjectOptions(contentType,
            Map.of("upload-fingerprint", fingerprintDigest), null);
        if (mode == OssUploadMode.SINGLE) {
            return new PreparedUpload(client.clientId(), bucket, key, null,
                client.presignPut(key, presignTtl, options));
        }
        OssMultipartUpload upload = client.createMultipartUpload(key, options);
        return new PreparedUpload(client.clientId(), bucket, key, upload.uploadId(), null);
    }

    @Override
    public List<OssUploadContracts.SignedPart> signParts(OssUploadTicket ticket, List<Integer> partNumbers,
                                                         Duration ttl) {
        OssClient client = client(ticket.service());
        return partNumbers.stream().map(number -> new OssUploadContracts.SignedPart(number,
            client.presignUploadPart(ticket.objectKey(), ticket.uploadId(), number, ttl))).toList();
    }

    @Override
    public OssPresignedRequest presignSingle(OssUploadTicket ticket, Duration ttl) {
        OssObjectOptions options = new OssObjectOptions(ticket.contentType(),
            Map.of("upload-fingerprint", ticket.fingerprintDigest()), null);
        return client(ticket.service()).presignPut(ticket.objectKey(), ttl, options);
    }

    @Override
    public List<OssMultipartPart> listParts(OssUploadTicket ticket) {
        return client(ticket.service()).listParts(ticket.objectKey(), ticket.uploadId());
    }

    @Override
    public void completeMultipart(OssUploadTicket ticket, List<OssCompletedPart> parts) {
        client(ticket.service()).completeMultipartUpload(ticket.objectKey(), ticket.uploadId(), parts);
    }

    @Override
    public Optional<OssObjectStat> headIfPresent(OssUploadTicket ticket) {
        try {
            return Optional.of(client(ticket.service()).headObject(ticket.objectKey()));
        } catch (S3StorageException e) {
            if (e.code() == OssErrorCode.OBJECT_NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public byte[] readPrefix(OssUploadTicket ticket, int length) {
        return client(ticket.service()).download(ticket.objectKey(), (result, input) -> {
            try {
                return input.readNBytes(length);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void abort(OssUploadCleanupRecord cleanup) {
        if (cleanup.mode() == OssUploadMode.MULTIPART && cleanup.uploadId() != null) {
            client(cleanup.service()).abortMultipartUpload(cleanup.objectKey(), cleanup.uploadId());
        }
    }

    @Override
    public void deleteObject(OssUploadCleanupRecord cleanup) {
        client(cleanup.service()).delete(cleanup.objectKey());
    }

    private OssClient client(String service) {
        return OssFactory.instance(service);
    }
}
