package org.namewta.common.notify.model;

import java.util.List;

/**
 * 渠道 Adapter 聚合结果。
 */
public record NotifyAdapterResult(String providerKey, List<NotifyTargetResult> deliveries) {

    public NotifyAdapterResult {
        deliveries = deliveries == null ? List.of() : List.copyOf(deliveries);
    }
}
