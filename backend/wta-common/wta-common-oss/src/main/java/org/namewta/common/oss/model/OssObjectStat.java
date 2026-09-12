package org.namewta.common.oss.model;

import java.time.Instant;
import java.util.Map;

/**
 * 对象 HEAD 结果。
 */
public record OssObjectStat(
    String bucket,
    String key,
    long size,
    String contentType,
    String eTag,
    Instant lastModified,
    Map<String, String> metadata,
    Map<OssChecksumAlgorithm, String> checksums
) {

    public OssObjectStat {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        checksums = checksums == null ? Map.of() : Map.copyOf(checksums);
    }
}
