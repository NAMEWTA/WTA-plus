package org.namewta.notify.api;

/**
 * 通知状态查询条件。
 *
 * @param notificationId 通知主键
 * @param includeContent 是否返回受保护内容
 */
public record NotificationQuery(String notificationId, boolean includeContent) { }
