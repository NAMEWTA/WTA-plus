package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.entity.NotifyIntentAttachment;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.port.NotifyAttachmentSnapshotPort.Prepared;
import org.namewta.system.api.OssService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 附件归属与状态规则；UseCase 分别开启短事务，适配器在事务外执行远端复制。 */
@Service
@RequiredArgsConstructor
public class NotifyAttachmentSnapshotService {
    private final NotifyNotificationDao dao;
    private final OssService ossService;

    /** 按 Intent 读取原提交附件序列；预约时仍须在锁内重验每条关系。 */
    public List<NotifyIntentAttachment> attachments(Long intentId) {
        return dao.attachments(intentId);
    }

    /** 仅提供有界回收候选，真正解除引用必须另进锁内事务。 */
    public List<NotifyIntentAttachment> attachmentReleaseCandidates(long afterId, int limit) {
        return dao.attachmentReleaseCandidates(afterId, limit);
    }

    /** Intent→关系锁下取得一次复制权；READY 可复用，COPYING/UNKNOWN 不盲重写。 */
    public Prepared reserve(Long intentId, Long relationId) {
        NotifyIntent intent = dao.lockIntent(intentId);
        if (intent == null || intent.getAttachmentActorUserId() == null
            || intent.getAttachmentActorClientPk() == null) throw new ServiceException("附件原提交者事实缺失");
        NotifyIntentAttachment relation = dao.lockAttachments(intentId).stream()
            .filter(item -> Objects.equals(item.getIntentAttachmentId(), relationId)).findFirst()
            .orElseThrow(() -> new ServiceException("附件关系不属于通知"));
        if ("READY".equals(relation.getStatus())) return new Prepared(relation, null);
        if (!"QUEUED".equals(relation.getStatus())) throw new ServiceException("附件复制结果未确认，需要人工核对");
        OssService.NotificationCopyReservation reservation = ossService.reserveNotificationSnapshot(
            relationId, relation.getSourceOssId(), intent.getAttachmentActorUserId(), intent.getAttachmentActorClientPk());
        relation.setSnapshotOssId(reservation.targetOssId());
        relation.setTargetService(reservation.targetService());
        relation.setTargetKey(reservation.targetKey());
        relation.setCopyToken(UUID.randomUUID().toString());
        relation.setStatus("COPYING");
        if (dao.saveAttachment(relation) != 1) throw new IllegalStateException("附件复制预约写入失败");
        return new Prepared(relation, reservation);
    }

    /** 复制已经返回，System 目标可下载与 Notify 关系 READY 必须同笔提交。 */
    public NotifyIntentAttachment confirm(Long intentId, Long relationId, String token,
                                          Prepared prepared, long copiedSize, String copiedSha256) {
        OssService.NotificationCopyReservation reservation = prepared.reservation();
        OssService.NotificationCopyResult result = new OssService.NotificationCopyResult(copiedSize, copiedSha256);
        if (dao.lockIntent(intentId) == null) throw new ServiceException("附件所属通知不存在");
        NotifyIntentAttachment relation = locked(intentId, relationId);
        if (!"COPYING".equals(relation.getStatus()) || !Objects.equals(relation.getCopyToken(), token)
            || !Objects.equals(relation.getSnapshotOssId(), reservation.targetOssId())) {
            throw new ServiceException("附件复制预约所有权已改变");
        }
        ossService.confirmNotificationSnapshot(reservation, result);
        relation.setSha256(result.sha256());
        relation.setStatus("READY");
        relation.setCopyToken(null);
        if (dao.saveAttachment(relation) != 1) throw new IllegalStateException("附件复制结果写入失败");
        return relation;
    }

    /** 远端或提交结果不确定时仅持久标记，不释放 owner/ref，不再自动重拷。 */
    public void uncertain(Long intentId, Long relationId, String token) {
        if (dao.lockIntent(intentId) == null) return;
        NotifyIntentAttachment relation = locked(intentId, relationId);
        if ("COPYING".equals(relation.getStatus()) && Objects.equals(token, relation.getCopyToken())) {
            relation.setStatus("COPY_UNKNOWN");
            if (dao.saveAttachment(relation) != 1) throw new IllegalStateException("附件未知状态写入失败");
        }
    }

    /**
     * 只有本聚合全部 MAIL 任务无活租约、都已结束且明确未发送，才解除共享快照/源引用。
     * COPY_UNKNOWN 和 COPYING 始终保留稳定目标，防迟到 PUT 在回收后制造无主对象。
     */
    public boolean releaseIfSafe(Long intentId) {
        NotifyIntent intent = dao.lockIntent(intentId);
        if (intent == null) return false;
        List<NotifyDelivery> visible = dao.deliveries(intentId);
        List<Long> ids = visible.stream().map(NotifyDelivery::getDeliveryId).sorted().toList();
        List<NotifyOutbox> outboxes = dao.lockOutboxes(intentId, ids);
        List<NotifyDelivery> deliveries = dao.lockDeliveries(intentId);
        List<NotifyIntentAttachment> relations = dao.lockAttachments(intentId);
        if (relations.isEmpty() || relations.stream().anyMatch(row ->
            !Boolean.FALSE.equals(row.getSendReserved())
                || !java.util.Arrays.asList("QUEUED", "READY", "RELEASED").contains(row.getStatus()))) return false;
        if (outboxes.stream().anyMatch(row -> !java.util.Arrays.asList("DONE", "DEAD_LETTER").contains(row.getStatus())
            || row.getLeaseOwner() != null || row.getLeaseToken() != null || row.getLeaseUntil() != null)) return false;
        for (NotifyDelivery delivery : deliveries) {
            if (!"MAIL".equals(delivery.getChannel())) continue;
            if (delivery.getProviderMessageId() != null || delivery.getAcceptedAt() != null) return false;
            if ("CANCELLED".equals(delivery.getStatus())
                || "UNDELIVERABLE".equals(delivery.getStatus())) continue;
            // 可人工重试的本地准备失败要保留来源；COPY_UNKNOWN 在前面已直接拒绝释放。
            if (!"FAILED".equals(delivery.getStatus()) || !java.util.Arrays.asList("NOTIFICATION_EXPIRED",
                "NOTICE_RETRACTED").contains(delivery.getErrorCode())) return false;
        }
        if (deliveries.stream().noneMatch(row -> "MAIL".equals(row.getChannel()))) return false;
        for (NotifyIntentAttachment relation : relations) {
            if ("RELEASED".equals(relation.getStatus())) continue;
            ossService.releaseNotificationReferences(relation.getIntentAttachmentId(),
                relation.getSourceOssId(), relation.getSnapshotOssId());
            relation.setStatus("RELEASED");
            if (dao.saveAttachment(relation) != 1) throw new IllegalStateException("附件引用解除失败");
        }
        return true;
    }

    private NotifyIntentAttachment locked(Long intentId, Long relationId) {
        return dao.lockAttachments(intentId).stream()
            .filter(item -> Objects.equals(item.getIntentAttachmentId(), relationId)).findFirst()
            .orElseThrow(() -> new ServiceException("附件关系不存在"));
    }
}
