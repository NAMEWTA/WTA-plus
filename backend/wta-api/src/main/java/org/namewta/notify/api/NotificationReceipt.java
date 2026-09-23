package org.namewta.notify.api;

import java.util.List;

/**
 * 通知提交结果。
 *
 * @param notificationId 通知主键
 * @param status 聚合状态
 * @param outboxQueued 是否进入 Outbox
 * @param followUpRequired 是否需要后续回执
 * @param deliveries 渠道投递结果
 */
public record NotificationReceipt(String notificationId, NotificationStatus status,
                                  boolean outboxQueued, boolean followUpRequired,
                                  List<DeliveryReceipt> deliveries) {
    public NotificationReceipt { deliveries = deliveries == null ? List.of() : List.copyOf(deliveries); }

    /**
     * 单渠道投递结果。
     *
     * @param recipientId 接收者标识
     * @param channel 渠道
     * @param status 投递状态
     * @param providerMessageId 供应商消息标识；REDACT_SENSITIVE 通知的公开投影为 null，内部回执关联保留原值
     */
    public record DeliveryReceipt(String recipientId, NotificationChannel channel,
                                  NotificationStatus status, String providerMessageId) { }
}
