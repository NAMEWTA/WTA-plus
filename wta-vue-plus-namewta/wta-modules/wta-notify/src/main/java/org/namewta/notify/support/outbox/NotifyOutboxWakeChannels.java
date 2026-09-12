package org.namewta.notify.support.outbox;

/**
 * Outbox 跨进程唤醒通道常量，供写入发布与 Worker 订阅共用。
 */
public final class NotifyOutboxWakeChannels {

    /**
     * Redis pub/sub 通道。载荷不含正文、收件人 PII 或 secret。
     */
    public static final String REDIS_CHANNEL = "notify:outbox:wake";

    private NotifyOutboxWakeChannels() {
    }
}
