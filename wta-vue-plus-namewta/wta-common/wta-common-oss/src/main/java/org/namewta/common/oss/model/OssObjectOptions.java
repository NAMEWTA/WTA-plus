package org.namewta.common.oss.model;

import java.util.Map;

/**
 * 直传对象的稳定选项。
 *
 * @param contentType       内容类型
 * @param metadata          对象元数据
 * @param checksumAlgorithm 可选的增强校验算法
 */
public record OssObjectOptions(
    String contentType,
    Map<String, String> metadata,
    OssChecksumAlgorithm checksumAlgorithm
) {

    public OssObjectOptions {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * 创建不附加内容属性的选项。
     */
    public static OssObjectOptions empty() {
        return new OssObjectOptions(null, Map.of(), null);
    }
}
