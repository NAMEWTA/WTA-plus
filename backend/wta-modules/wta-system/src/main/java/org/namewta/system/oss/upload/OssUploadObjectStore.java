package org.namewta.system.oss.upload;

import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssCompletedPart;
import org.namewta.common.oss.model.OssMultipartPart;
import org.namewta.common.oss.model.OssObjectStat;
import org.namewta.common.oss.model.OssPresignedRequest;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 上传控制面使用的 Provider 接缝。
 */
public interface OssUploadObjectStore {

    PreparedUpload prepare(String storageConfigKey, AccessPolicy expectedAccessPolicy, String objectPrefix,
                           String fileName, String contentType, String fingerprintDigest, OssUploadMode mode,
                           Duration presignTtl);

    List<OssUploadContracts.SignedPart> signParts(OssUploadTicket ticket, List<Integer> partNumbers, Duration ttl);

    OssPresignedRequest presignSingle(OssUploadTicket ticket, Duration ttl);

    List<OssMultipartPart> listParts(OssUploadTicket ticket);

    void completeMultipart(OssUploadTicket ticket, List<OssCompletedPart> parts);

    Optional<OssObjectStat> headIfPresent(OssUploadTicket ticket);

    byte[] readPrefix(OssUploadTicket ticket, int length);

    void abort(OssUploadCleanupRecord cleanup);

    void deleteObject(OssUploadCleanupRecord cleanup);

    record PreparedUpload(
        String service,
        String bucket,
        String objectKey,
        String uploadId,
        OssPresignedRequest presignedRequest
    ) {
    }
}
