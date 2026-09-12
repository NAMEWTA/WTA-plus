package org.namewta.common.notify.attachment;

/**
 * 通知附件资源描述符，不包含临时访问凭据。
 */
public record NotifyAttachmentResource(
    Long ossId,
    String fileName,
    String contentType,
    long size,
    NotifyAttachmentMaterializer materializer
) {
}
