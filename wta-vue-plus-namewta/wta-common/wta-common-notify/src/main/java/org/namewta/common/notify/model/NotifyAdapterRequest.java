package org.namewta.common.notify.model;

import org.namewta.common.notify.attachment.NotifyAttachmentSnapshot;

import java.util.List;

/**
 * 渠道 Adapter 输入。
 */
public record NotifyAdapterRequest(
    NotifyRequest request,
    NotifyContext context,
    List<NotifyAttachmentSnapshot> attachments
) {

    public NotifyAdapterRequest {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public NotifyAdapterRequest(NotifyRequest request, NotifyContext context) {
        this(request, context, List.of());
    }
}
