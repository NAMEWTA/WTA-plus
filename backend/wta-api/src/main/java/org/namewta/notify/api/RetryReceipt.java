package org.namewta.notify.api;

/**
 * 通知重试结果。
 *
 * @param notificationId 通知主键
 * @param status 状态
 * @param queuedCount 本次实际重新排队的投递数；零表示持久状态未改变
 */
public record RetryReceipt(String notificationId, NotificationStatus status, int queuedCount) { }
