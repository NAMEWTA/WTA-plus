package org.namewta.notify.port;

import org.namewta.notify.domain.entity.NotifyIntentAttachment;
import org.namewta.system.api.OssService;

import java.util.List;

/** 附件关系读取与独立短事务入口；调用方须在预约和确认之间执行远端复制。 */
public interface NotifyAttachmentSnapshotPort {
    /** 读取同一 Intent 按提交顺序保存的附件关系。 */
    List<NotifyIntentAttachment> attachments(Long intentId);

    /** 分页查找可能可回收的关系；是否安全仍由锁内 releaseIfSafe 判定。 */
    List<NotifyIntentAttachment> attachmentReleaseCandidates(long afterId, int limit);

    /** 在短事务中取得或复用私有快照预约。 */
    Prepared reserve(Long intentId, Long relationId);

    /** 复制返回后，在独立短事务中确认目标与关系。 */
    NotifyIntentAttachment confirm(Long intentId, Long relationId, String token,
                                   Prepared prepared, long copiedSize, String copiedSha256);

    /** 远端或提交结果不确定时，保守保留关系与引用。 */
    void uncertain(Long intentId, Long relationId, String token);

    /** 仅在全部投递已确定未发且无活租约时解除本 Intent 的共享引用。 */
    boolean releaseIfSafe(Long intentId);

    /** 事务中保存的关系与稳定目标预约，不包含复制结果。 */
    record Prepared(NotifyIntentAttachment relation, OssService.NotificationCopyReservation reservation) { }
}
