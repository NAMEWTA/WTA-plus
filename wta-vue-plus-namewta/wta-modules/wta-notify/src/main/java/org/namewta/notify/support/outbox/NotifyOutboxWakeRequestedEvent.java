package org.namewta.notify.support.outbox;

/**
 * 当前动态数据源事务内已写出可 claim Outbox 后的提交后唤醒请求。
 *
 * @param outboxId 可选 Outbox 主键 hint，允许为 {@code null}
 */
public record NotifyOutboxWakeRequestedEvent(Long outboxId) {
}
