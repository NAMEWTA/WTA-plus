package org.namewta.common.notify.model;

import java.util.List;

/**
 * 一次逻辑通知的同步结果。
 */
public record NotifyResult(
    String requestId,
    NotifyChannel channel,
    String providerKey,
    NotifyStatus status,
    List<NotifyTargetResult> deliveries
) {

    public NotifyResult {
        deliveries = deliveries == null ? List.of() : List.copyOf(deliveries);
    }
}
