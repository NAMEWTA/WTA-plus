package org.namewta.common.notify.model;

/**
 * 一次逻辑通知的同步状态。
 */
public enum NotifyStatus {
    ACCEPTED,
    PARTIAL_FAILURE,
    FAILED,
    SKIPPED_DUPLICATE
}
