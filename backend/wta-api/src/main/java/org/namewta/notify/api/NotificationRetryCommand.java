package org.namewta.notify.api;

/**
 * 通知重试命令。
 *
 * @param notificationId 通知主键
 * @param deliveryId 可选投递主键
 * @param reason 重试原因
 * @param idempotencyKey 尚无独立幂等结果存储，当前仅接受空值
 */
public record NotificationRetryCommand(String notificationId, String deliveryId, String reason, String idempotencyKey) { }
