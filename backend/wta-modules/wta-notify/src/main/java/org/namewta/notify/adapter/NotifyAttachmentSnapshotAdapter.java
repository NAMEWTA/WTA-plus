package org.namewta.notify.adapter;

import lombok.RequiredArgsConstructor;
import org.namewta.common.notify.attachment.NotifyAttachmentResource;
import org.namewta.common.notify.attachment.NotifyAttachmentSnapshot;
import org.namewta.common.notify.attachment.NotifyAttachmentSnapshotService;
import org.namewta.common.notify.exception.NotifyAttachmentSnapshotException;
import org.namewta.common.notify.model.NotifyContext;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyIntentAttachment;
import org.namewta.notify.service.runtime.NotifyAttachmentSnapshotTransactions;
import org.namewta.system.api.OssService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 按真实 Intent owner 复用一组私有快照，单条投递失败不删除共享资源。 */
@Component
@RequiredArgsConstructor
public class NotifyAttachmentSnapshotAdapter implements NotifyAttachmentSnapshotService {
    private final NotifyNotificationDao dao;
    private final NotifyAttachmentSnapshotTransactions transactions;
    private final OssService ossService;

    @Override
    public List<NotifyAttachmentSnapshot> createSnapshots(long intentId, List<Long> sourceOssIds, NotifyContext context) {
        if (intentId <= 0 || sourceOssIds == null || sourceOssIds.isEmpty()) {
            throw new NotifyAttachmentSnapshotException("ATTACHMENT_OWNER_REQUIRED", "附件归属通知不存在");
        }
        List<NotifyIntentAttachment> relations = dao.attachments(intentId);
        if (!relations.stream().map(NotifyIntentAttachment::getSourceOssId).toList().equals(sourceOssIds)) {
            throw new NotifyAttachmentSnapshotException("ATTACHMENT_SOURCE_MISMATCH", "附件序列与持久归属不一致");
        }
        List<NotifyAttachmentSnapshot> result = new ArrayList<>(relations.size());
        for (NotifyIntentAttachment relation : relations) {
            NotifyAttachmentSnapshotTransactions.Prepared prepared;
            try {
                prepared = transactions.reserve(intentId, relation.getIntentAttachmentId());
            } catch (RuntimeException exception) {
                throw new NotifyAttachmentSnapshotException("ATTACHMENT_RESERVATION_FAILED", "附件私有快照预约失败", exception);
            }
            NotifyIntentAttachment current = prepared.relation();
            if (prepared.reservation() != null) {
                try {
                    OssService.NotificationCopyResult copied = ossService.copyNotificationSnapshot(prepared.reservation());
                    current = transactions.confirm(intentId, relation.getIntentAttachmentId(), current.getCopyToken(),
                        prepared.reservation(), copied);
                } catch (RuntimeException exception) {
                    try { transactions.uncertain(intentId, relation.getIntentAttachmentId(), current.getCopyToken()); }
                    catch (RuntimeException cannotRecord) {
                        if (cannotRecord != exception) exception.addSuppressed(cannotRecord);
                    }
                    throw new NotifyAttachmentSnapshotException("ATTACHMENT_COPY_UNCERTAIN",
                        "附件私有快照结果未确认", exception);
                }
            }
            if (!"READY".equals(current.getStatus()) || current.getSnapshotOssId() == null) {
                throw new NotifyAttachmentSnapshotException("ATTACHMENT_NOT_READY", "附件私有快照未就绪");
            }
            Long targetId = current.getSnapshotOssId();
            Long sourceId = current.getSourceOssId();
            Long ownerRelationId = current.getIntentAttachmentId();
            String expectedSha256 = current.getSha256();
            result.add(new NotifyAttachmentSnapshot(current.getSourceOssId(),
                new NotifyAttachmentResource(targetId, current.getFileName(), current.getContentType(),
                    current.getFileSize(), path -> ossService.materializeNotificationSnapshot(
                        ownerRelationId, sourceId, targetId, expectedSha256, path))));
        }
        return List.copyOf(result);
    }

    @Override
    public void cleanupSnapshots(List<NotifyAttachmentSnapshot> snapshots) {
        // 一份 Intent 可供多条 MAIL delivery 共用；仅有安全证明的 owner 回收流程能解引用。
        // 普通单条失败、UNKNOWN 或 SMTP ACK 丢失绝不在这里删除对象。
    }
}
