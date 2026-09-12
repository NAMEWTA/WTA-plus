package org.namewta.common.oss.model;

import java.util.List;

/**
 * Bucket 直传前置配置的只读诊断结果。
 */
public record OssBucketConfiguration(
    String bucket,
    boolean explicitCorsOrigins,
    boolean corsAllowsPut,
    boolean corsExposesEtag,
    boolean abortIncompleteMultipartUpload,
    List<String> issues
) {

    public OssBucketConfiguration {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean compliant() {
        return explicitCorsOrigins && corsAllowsPut && corsExposesEtag
            && abortIncompleteMultipartUpload && issues.isEmpty();
    }
}
