package org.namewta.common.notify.event;

import org.namewta.common.notify.model.NotifyContext;
import org.namewta.common.notify.model.NotifyRequest;
import org.namewta.common.notify.model.NotifyResult;

import java.time.Instant;
import java.util.List;

/**
 * Provider 同步调用后的不可变监控事件。
 */
public record NotifyDeliveryEvent(
    NotifyRequest request,
    NotifyContext context,
    NotifyResult result,
    String originalRequestId,
    Long notifyLogId,
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
