package org.namewta.notify.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.service.runtime.NotifyAttachmentSnapshotTransactions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** 有界轮询已终结意图；未发送事实由每个聚合的短事务重新核实。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyAttachmentReleaseJob {
    private final NotifyNotificationDao dao;
    private final NotifyAttachmentSnapshotTransactions transactions;
    private final AtomicLong cursor = new AtomicLong();

    @Scheduled(fixedDelay = 60_000)
    public void releaseEligible() {
        var batch = dao.attachmentReleaseCandidates(cursor.get(), 100);
        if (batch.isEmpty()) {
            cursor.set(0);
            return;
        }
        for (var row : batch) {
            cursor.set(row.getIntentAttachmentId());
            try { transactions.releaseIfSafe(row.getIntentId()); }
            catch (RuntimeException exception) {
                log.warn("通知附件安全回收稍后重试，异常类别={}", exception.getClass().getSimpleName());
            }
        }
    }
}
