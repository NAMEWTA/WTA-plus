package org.namewta.notify.api;

/**
 * 通知取消结果。
 *
 * @param notificationId 通知主键
 * @param status 状态
 */
public record CancelReceipt(String notificationId, NotificationStatus status) { }
