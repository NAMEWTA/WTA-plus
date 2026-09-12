package org.namewta.notify.domain.policy;

import org.namewta.common.core.exception.ServiceException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** 公告草稿的发送范围规则；空指定目标绝不能退化为全体用户。 */
public final class NoticeAudiencePolicy {
    private NoticeAudiencePolicy() { }

    /** 已规范化且不可变的草稿目标和渠道。 */
    public record Audience(String recipientType, List<Long> recipientIds, List<Long> userTypeIds, List<String> channels) { }

    public static Audience normalize(String type, List<Long> recipients, List<Long> userTypes, List<String> channels) {
        String normalizedType = type == null ? "ALL" : type.trim().toUpperCase(Locale.ROOT);
        List<Long> ids = normalizeIds(recipients);
        List<Long> types = normalizeIds(userTypes);
        boolean valid = switch (normalizedType) {
            case "ALL" -> ids.isEmpty() && types.isEmpty();
            case "USER" -> !ids.isEmpty() && types.isEmpty();
            case "USER_TYPE" -> ids.isEmpty() && !types.isEmpty();
            default -> false;
        };
        if (!valid) throw new ServiceException("发送对象类型与所选目标不匹配，指定用户或用户类型不能为空");
        List<String> normalizedChannels = channels == null ? List.of("IN_APP") : channels;
        if (normalizedChannels.isEmpty() || normalizedChannels.stream().anyMatch(value -> value == null
            || !List.of("IN_APP", "SMS", "MAIL").contains(value.trim().toUpperCase(Locale.ROOT)))) {
            throw new ServiceException("至少选择一个已实现的渠道：站内信、短信或邮件");
        }
        return new Audience(normalizedType, ids, types, normalizedChannels.stream()
            .map(value -> value.trim().toUpperCase(Locale.ROOT)).distinct().toList());
    }

    private static List<Long> normalizeIds(List<Long> values) {
        if (values == null) return List.of();
        if (values.stream().anyMatch(id -> id == null || id <= 0)) throw new ServiceException("发送目标编号必须为正数");
        return List.copyOf(new LinkedHashSet<>(values));
    }
}
