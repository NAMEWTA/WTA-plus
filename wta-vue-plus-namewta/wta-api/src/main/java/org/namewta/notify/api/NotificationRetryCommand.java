package org.namewta.notify.api;

/**
 * 通知重试命令。
 *
 * @param notificationId 通知主键
 * @param deliveryId 可选投递主键
 * @param reason 重试原因
 * @param idempotencyKey 重试幂等键
 */
public record NotificationRetryCommand(String notificationId, String deliveryId, String reason, String idempotencyKey) { }
