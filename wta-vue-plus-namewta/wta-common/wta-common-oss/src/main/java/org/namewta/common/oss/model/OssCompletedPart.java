package org.namewta.common.oss.model;

import java.util.Map;

/**
 * 完成 Multipart Upload 时提交的 Part 描述。
 */
public record OssCompletedPart(
    int partNumber,
    String eTag,
    Map<OssChecksumAlgorithm, String> checksums
) {

    public OssCompletedPart {
        checksums = checksums == null ? Map.of() : Map.copyOf(checksums);
    }
}
