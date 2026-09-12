package org.namewta.notify.api;

/**
 * 通知重试结果。
 *
 * @param notificationId 通知主键
 * @param status 状态
 */
public record RetryReceipt(String notificationId, NotificationStatus status) { }
