package org.namewta.common.oss.model;

import java.util.Set;

/**
 * OSS Provider 能力快照。
 *
 * @param multipartUpload   是否支持 Multipart Upload
 * @param copyObject        是否支持服务端对象复制
 * @param checksumAlgorithms       已确认支持的增强校验算法
 * @param readOnlyAccessDiagnostic 是否支持只读访问边界诊断
 */
public record OssClientCapabilities(
    boolean multipartUpload,
    boolean copyObject,
    Set<OssChecksumAlgorithm> checksumAlgorithms,
    boolean readOnlyAccessDiagnostic
) {

    public OssClientCapabilities {
        checksumAlgorithms = checksumAlgorithms == null ? Set.of() : Set.copyOf(checksumAlgorithms);
    }

    /**
     * 返回所有 S3-compatible Provider 必须满足的基础能力，不宣称可选 checksum 能力。
     */
    public static OssClientCapabilities s3CompatibleBaseline() {
        return new OssClientCapabilities(true, true, Set.of(), true);
    }

    /**
     * 判断是否已确认支持指定增强校验算法。
     *
     * @param algorithm 校验算法
     * @return 是否支持
     */
    public boolean supportsChecksum(OssChecksumAlgorithm algorithm) {
        return checksumAlgorithms.contains(algorithm);
    }
}
