package org.namewta.notify.api;

/**
 * 通知聚合状态。
 */
public enum NotificationStatus { QUEUED, PROCESSING, ACCEPTED, PARTIAL_FAILURE, DELIVERED, UNDELIVERABLE, UNKNOWN, FAILED, CANCELLED, EXPIRED }
