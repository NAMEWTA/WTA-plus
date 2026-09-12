package org.namewta.notify.domain.policy;

import org.namewta.notify.api.NotificationStatus;

import java.util.List;

/**
 * 通知聚合状态策略，区分供应商接受、实际送达、失败和待回执。
 */
public final class NotificationAggregatePolicy {
    private NotificationAggregatePolicy() {
    }

    /**
     * 根据所有渠道投递状态计算通知聚合状态。
     *
     * @param statuses 投递状态
     * @return 聚合状态
     */
    public static NotificationStatus aggregate(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return NotificationStatus.UNKNOWN;
        long cancelled = statuses.stream().filter("CANCELLED"::equals).count();
        long effective = statuses.size() - cancelled;
        if (effective == 0) return NotificationStatus.CANCELLED;
        if (statuses.stream().anyMatch(status -> "PENDING".equals(status))) return NotificationStatus.PROCESSING;
        if (statuses.stream().filter("DELIVERED"::equals).count() == effective) return NotificationStatus.DELIVERED;
        if (statuses.stream().anyMatch(status -> "UNKNOWN".equals(status) || "WAITING_RECEIPT".equals(status))) {
            return NotificationStatus.UNKNOWN;
        }
        long successful = statuses.stream().filter(status -> "ACCEPTED".equals(status) || "DELIVERED".equals(status)).count();
        long failed = statuses.stream().filter(status -> "FAILED".equals(status) || "UNDELIVERABLE".equals(status)).count();
        if (successful > 0 && failed > 0) return NotificationStatus.PARTIAL_FAILURE;
        if (successful == effective) return NotificationStatus.ACCEPTED;
        return NotificationStatus.FAILED;
    }
}
