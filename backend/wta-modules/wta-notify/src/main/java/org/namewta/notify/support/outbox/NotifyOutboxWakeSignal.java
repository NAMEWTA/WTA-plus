package org.namewta.notify.support.outbox;

import java.io.Serial;
import java.io.Serializable;

/**
 * 跨进程 Outbox 唤醒载荷。
 *
 * <p>只携带调度提示；{@code outboxId} 仅为可选 hint，Worker 仍必须走 claim/lease。</p>
 *
 * @param type     信号类型，固定为 {@link #TYPE_WAKE}
 * @param outboxId 可选 Outbox 主键 hint，允许为 {@code null}
 */
public record NotifyOutboxWakeSignal(String type, Long outboxId) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 唤醒信号类型。 */
    public static final String TYPE_WAKE = "WAKE";

    /**
     * 构造不含业务字段的唤醒载荷。
     *
     * @param outboxId 可选 Outbox 主键 hint
     * @return 唤醒信号
     */
    public static NotifyOutboxWakeSignal wake(Long outboxId) {
        return new NotifyOutboxWakeSignal(TYPE_WAKE, outboxId);
    }
}
