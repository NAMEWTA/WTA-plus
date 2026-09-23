package org.namewta.notify.api;

/**
 * 通知排队模式；当前仅 ASYNC，提交只持久化意图和 Outbox，供应商投递由 Worker 执行。
 */
public enum NotificationMode { ASYNC }
