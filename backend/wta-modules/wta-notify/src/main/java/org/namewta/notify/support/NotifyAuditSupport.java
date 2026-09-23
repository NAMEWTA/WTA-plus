package org.namewta.notify.support;

import org.namewta.common.json.utils.JsonUtils;
import org.namewta.notify.domain.entity.NotifyIntent;

import java.util.Map;

/** 将持久化审计策略转换为事件策略和公开投影策略。缺失或损坏的 Intent 保守脱敏。 */
public final class NotifyAuditSupport {
    private NotifyAuditSupport() { }

    /** 无或空 metadata 沿用既有 FULL；策略未知、损坏或 Intent 缺失时脱敏，避免读路径泄漏。 */
    public static boolean redactSensitive(NotifyIntent intent) {
        if (intent == null) return true;
        String json = intent.getMetadataJson();
        if (json == null || json.isBlank()) return false;
        try {
            Map<String, Object> metadata = JsonUtils.parseObject(json, Map.class);
            if (metadata == null) return true;
            Object requested = metadata.get("audit");
            return requested != null && !"FULL".equals(requested);
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    /** 只隐藏公开投影中的供应商消息标识；持久化原值仍供回执关联。 */
    public static String publicProviderMessageId(NotifyIntent intent, String providerMessageId) {
        return redactSensitive(intent) ? null : providerMessageId;
    }
}
