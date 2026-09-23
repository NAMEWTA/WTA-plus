package org.namewta.common.notify.model;

/**
 * 单个物理目标的 Provider attempt 结果。
 */
public record NotifyTargetResult(
    NotifyTarget target,
    NotifyDeliveryStatus status,
    String providerMessageId,
    String errorCode,
    String errorMessage,
    long costTime
) {

    public static NotifyTargetResult accepted(NotifyTarget target, String providerMessageId, long costTime) {
        return new NotifyTargetResult(target, NotifyDeliveryStatus.ACCEPTED, providerMessageId, null, null, costTime);
    }

    public static NotifyTargetResult failed(NotifyTarget target, String errorCode, String errorMessage, long costTime) {
        return new NotifyTargetResult(target, NotifyDeliveryStatus.FAILED, null, errorCode, errorMessage, costTime);
    }

    public static NotifyTargetResult unsentRetryable(NotifyTarget target, String errorCode, long costTime) {
        return new NotifyTargetResult(target, NotifyDeliveryStatus.UNSENT_RETRYABLE, null, errorCode,
            "Provider 明确未受理", costTime);
    }

    public static NotifyTargetResult unsentTerminal(NotifyTarget target, String errorCode, long costTime) {
        return new NotifyTargetResult(target, NotifyDeliveryStatus.UNSENT_TERMINAL, null, errorCode,
            "Provider 明确未受理", costTime);
    }

    public static NotifyTargetResult outcomeUnknown(NotifyTarget target, long costTime) {
        return new NotifyTargetResult(target, NotifyDeliveryStatus.OUTCOME_UNKNOWN, null,
            "PROVIDER_OUTCOME_UNKNOWN", "Provider 调用结果未知", costTime);
    }
}
