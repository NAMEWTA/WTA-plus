package org.namewta.common.notify.model;

/**
 * 单个 Provider attempt 状态。
 */
public enum NotifyDeliveryStatus {
    ACCEPTED,
    /** 供应商明确未受理，且同一目标允许在 Outbox 预算内再次实际调用。 */
    UNSENT_RETRYABLE,
    /** 供应商明确未受理，且相同请求不能通过等待自行恢复。 */
    UNSENT_TERMINAL,
    /** 已调用外部边界，但是否受理不可确定；绝不可盲重发。 */
    OUTCOME_UNKNOWN,
    /** 旧的未分类失败；不赋予重发权。 */
    FAILED
}
