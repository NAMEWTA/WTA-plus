package org.namewta.notify.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyIntentAttachment;
import org.namewta.notify.port.NotifyAttachmentSnapshotPort;
import org.namewta.notify.service.runtime.NotifyAttachmentSnapshotService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 附件预约、确认、未知与回收的 Spring 代理事务入口；远端复制由适配器在事务外执行。 */
@Service
@RequiredArgsConstructor
public class NotifyAttachmentSnapshotUseCase implements NotifyAttachmentSnapshotPort {
    private final NotifyAttachmentSnapshotService service;

    @Override
    public List<NotifyIntentAttachment> attachments(Long intentId) {
        return service.attachments(intentId);
    }

    @Override
    public List<NotifyIntentAttachment> attachmentReleaseCandidates(long afterId, int limit) {
        return service.attachmentReleaseCandidates(afterId, limit);
    }

    @Override
    @DSTransactional
    public Prepared reserve(Long intentId, Long relationId) {
        return service.reserve(intentId, relationId);
    }

    @Override
    @DSTransactional
    public NotifyIntentAttachment confirm(Long intentId, Long relationId, String token,
                                          Prepared prepared, long copiedSize, String copiedSha256) {
        return service.confirm(intentId, relationId, token, prepared, copiedSize, copiedSha256);
    }

    @Override
    @DSTransactional
    public void uncertain(Long intentId, Long relationId, String token) {
        service.uncertain(intentId, relationId, token);
    }

    @Override
    @DSTransactional
    public boolean releaseIfSafe(Long intentId) {
        return service.releaseIfSafe(intentId);
    }
}
