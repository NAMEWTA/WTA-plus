package org.namewta.notify.api;

/**
 * 取消尚未完成通知命令。
 *
 * @param notificationId 通知主键
 * @param reason 取消原因
 */
public record NotificationCancelCommand(String notificationId, String reason) { }
