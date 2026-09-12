package org.namewta.common.oss.model;

import java.time.Instant;
import java.util.Map;

/**
 * Provider 已保存的 Multipart Part。
 */
public record OssMultipartPart(
    int partNumber,
    String eTag,
    long size,
    Instant lastModified,
    Map<OssChecksumAlgorithm, String> checksums
) {

    public OssMultipartPart {
        checksums = checksums == null ? Map.of() : Map.copyOf(checksums);
    }
}
