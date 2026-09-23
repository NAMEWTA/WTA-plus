package org.namewta.notify.api;

/**
 * 多渠道通知编排策略；新提交只支持 ALL，其他值仅保留供历史记录读取。
 */
public enum NotificationStrategy { ALL, ORDERED_FALLBACK, ESCALATION }
