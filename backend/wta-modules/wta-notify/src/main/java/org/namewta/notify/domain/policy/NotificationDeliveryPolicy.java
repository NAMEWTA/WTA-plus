package org.namewta.notify.domain.policy;

import java.util.Set;

/** 回调与投递完成共享的单调状态规则；人工重试由独立用例显式恢复 PENDING。 */
public final class NotificationDeliveryPolicy {
    /** 纯静态策略禁止实例化。 */
    private NotificationDeliveryPolicy() { }

    /** 终态不可被较晚的 Provider 响应或低等级回执覆盖。 */
    public static boolean canAdvance(String current, String next) {
        if (current == null || current.isBlank()) return true;
        if (Set.of("DELIVERED", "CANCELLED", "FAILED", "UNDELIVERABLE").contains(current)) return false;
        return rank(next) > rank(current);
    }

    /** 仅比较回执确定性，不将 UNKNOWN 当作送达。 */
    private static int rank(String status) {
        if (status == null) return 0;
        return switch (status) {
            case "DELIVERED" -> 4;
            case "UNDELIVERABLE", "FAILED" -> 3;
            case "ACCEPTED" -> 2;
            default -> 1;
        };
    }
}
