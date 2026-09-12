package org.namewta.common.oss.model;

import java.util.Map;

/**
 * Multipart Upload 完成结果。ETag 仅作为 Provider 标识，不代表整文件 MD5。
 */
public record OssMultipartCompleteResult(
    String bucket,
    String key,
    String eTag,
    Map<OssChecksumAlgorithm, String> checksums
) {

    public OssMultipartCompleteResult {
        checksums = checksums == null ? Map.of() : Map.copyOf(checksums);
    }
}
