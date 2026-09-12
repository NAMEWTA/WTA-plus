package org.namewta.common.notify.attachment;

/**
 * 源附件与通知专用快照的对应关系。
 */
public record NotifyAttachmentSnapshot(
    Long sourceOssId,
    NotifyAttachmentResource resource
) {
}
