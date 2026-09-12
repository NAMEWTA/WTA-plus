package org.namewta.common.oss.model;

import java.time.Instant;

/**
 * 服务端对象复制结果。
 */
public record OssCopyResult(
    String sourceBucket,
    String sourceKey,
    String targetBucket,
    String targetKey,
    String eTag,
    Instant lastModified
) {
}
