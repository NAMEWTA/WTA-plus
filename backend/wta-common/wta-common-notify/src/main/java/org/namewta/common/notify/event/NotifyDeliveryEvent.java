package org.namewta.common.notify.event;

import org.namewta.common.notify.model.NotifyContext;
import org.namewta.common.notify.model.NotifyRequest;
import org.namewta.common.notify.model.NotifyResult;

import java.time.Instant;
import java.util.List;

/**
 * Provider 同步调用后的不可变监控事件。REDACT_SENSITIVE 请求的事件包含脱敏副本，
 * 不可用其字段重试、重新计算幂等摘要或重建供应商调用。
 */
public record NotifyDeliveryEvent(
    NotifyRequest request,
    NotifyContext context,
    NotifyResult result,
    String originalRequestId,
    Long attachmentOwnerIntentId,
    List<Long> attachmentSnapshotOssIds,
    Instant occurredAt
) {

    public NotifyDeliveryEvent {
        attachmentSnapshotOssIds = attachmentSnapshotOssIds == null
            ? List.of() : List.copyOf(attachmentSnapshotOssIds);
    }

    public NotifyDeliveryEvent(NotifyRequest request, NotifyContext context, NotifyResult result,
                               Instant occurredAt) {
        this(request, context, result, null, null, List.of(), occurredAt);
    }

    public NotifyDeliveryEvent(NotifyRequest request, NotifyContext context, NotifyResult result,
                               String originalRequestId, Instant occurredAt) {
        this(request, context, result, originalRequestId, null, List.of(), occurredAt);
    }
}
