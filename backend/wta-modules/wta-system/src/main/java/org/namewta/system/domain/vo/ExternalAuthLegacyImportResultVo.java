package org.namewta.system.domain.vo;

import java.util.List;

/** 旧配置预览或执行结果；不包含密钥、密文或完整扩展参数。 */
public record ExternalAuthLegacyImportResultVo(List<Item> items) {
    public ExternalAuthLegacyImportResultVo {
        items = List.copyOf(items);
    }
    /** 单条导入结果；status 为 READY/SKIPPED/EXISTS/IMPORTED/INVALID。 */
    public record Item(String source, String providerKey, String externalClientId, String redirectUri,
                       boolean secretConfigured, String status, String message, Long registrationId) {
    }
}
