package org.namewta.common.notify.attachment;

import org.namewta.common.notify.model.NotifyContext;

import java.util.List;

/**
 * 通知附件快照 SPI，由应用层存储模块实现。
 */
public interface NotifyAttachmentSnapshotService {

    /** ownerIntentId 是持久 Intent 主键，同一意图的多条投递必须复用快照。 */
    List<NotifyAttachmentSnapshot> createSnapshots(long ownerIntentId, List<Long> sourceOssIds, NotifyContext context);

    void cleanupSnapshots(List<NotifyAttachmentSnapshot> snapshots);
}
