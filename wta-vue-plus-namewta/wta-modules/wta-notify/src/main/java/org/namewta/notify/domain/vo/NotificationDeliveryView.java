package org.namewta.notify.domain.vo;

import org.namewta.notify.domain.entity.NotifyDelivery;

import java.time.LocalDateTime;

/**
 * 通知监控安全投影，不暴露内部实体和敏感目标。
 */
public record NotificationDeliveryView(Long deliveryId, Long intentId, Long userId, String channel,
                                       String status, Integer attemptCount, String providerMessageId,
                                       String errorCode, LocalDateTime acceptedAt, LocalDateTime deliveredAt,
                                       LocalDateTime readAt, LocalDateTime createTime) {
    /** 将持久化实体映射为监控投影。 */
    public static NotificationDeliveryView from(NotifyDelivery item) {
        return new NotificationDeliveryView(item.getDeliveryId(), item.getIntentId(), item.getUserId(),
            item.getChannel(), item.getStatus(), item.getAttemptCount(), item.getProviderMessageId(),
            item.getErrorCode(), item.getAcceptedAt(), item.getDeliveredAt(), item.getReadAt(), item.getCreateTime());
    }
}
