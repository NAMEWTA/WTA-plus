package org.namewta.common.notify.model;

import java.util.Map;

/**
 * Provider 模板通知内容。
 */
public record NotifyTemplateContent(
    String subject,
    String providerTemplateCode,
    Map<String, String> params,
    String contentSnapshot
) implements NotifyContent {

    public NotifyTemplateContent {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
