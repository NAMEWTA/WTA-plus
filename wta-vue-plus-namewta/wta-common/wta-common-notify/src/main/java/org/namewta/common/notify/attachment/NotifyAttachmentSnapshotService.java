package org.namewta.common.notify.attachment;

import org.namewta.common.notify.model.NotifyContext;

import java.util.List;

/**
 * 通知附件快照 SPI，由应用层存储模块实现。
 */
public interface NotifyAttachmentSnapshotService {

    List<NotifyAttachmentSnapshot> createSnapshots(long notifyLogId, List<Long> sourceOssIds, NotifyContext context);

    void cleanupSnapshots(List<NotifyAttachmentSnapshot> snapshots);
}
