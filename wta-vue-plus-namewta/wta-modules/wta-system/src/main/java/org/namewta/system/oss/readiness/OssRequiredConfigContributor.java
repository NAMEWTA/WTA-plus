package org.namewta.system.oss.readiness;

import java.util.Map;
import java.util.Set;

/**
 * 为进行中的业务流程贡献必须可服务的 OSS 配置。
 */
public interface OssRequiredConfigContributor {

    /**
     * 返回 configKey 到非敏感来源类别的映射。
     */
    Map<String, Set<String>> requiredConfigs();
}
