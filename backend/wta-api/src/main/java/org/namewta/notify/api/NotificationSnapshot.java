package org.namewta.notify.api;

import java.time.Instant;
import java.util.List;

/**
 * 通知状态快照。
 *
 * @param notificationId 通知主键
 * @param status 聚合状态
 * @param createdAt 创建时间
 * @param deliveries 投递明细
 */
public record NotificationSnapshot(String notificationId, NotificationStatus status, Instant createdAt,
                                   List<NotificationReceipt.DeliveryReceipt> deliveries) {
    public NotificationSnapshot { deliveries = deliveries == null ? List.of() : List.copyOf(deliveries); }
}
