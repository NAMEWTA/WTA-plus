package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Outbox 领取服务，以短事务锁定任务并建立租约。
 */
@Service
@RequiredArgsConstructor
public class NotifyOutboxClaimService {
    private final NotifyNotificationDao dao;

    /**
     * 领取到期任务；事务结束后才能执行供应商 I/O。
     *
     * @param owner 当前 Worker 标识
     * @return 已建立租约的任务
     */
    public List<NotifyOutbox> claim(String owner) {
        LocalDateTime nowUtc = dao.databaseNow();
        LocalDateTime leaseUntil = nowUtc.plusSeconds(60);
        List<NotifyOutbox> candidates = dao.claimCandidates(nowUtc, 50);
        var channels = dao.deliveryChannels(candidates.stream().map(NotifyOutbox::getDeliveryId)
            .filter(Objects::nonNull).distinct().toList());
        return candidates.stream().filter(outbox -> {
            String token = UUID.randomUUID().toString();
            if (dao.claimOutbox(outbox.getOutboxId(), owner, token, leaseUntil, nowUtc,
                outbox.getDeliveryId() != null && "IN_APP".equals(channels.get(outbox.getDeliveryId()))) != 1) return false;
            outbox.setClaimedFromReady("READY".equals(outbox.getStatus()));
            outbox.setStatus("PROCESSING");
            outbox.setLeaseOwner(owner);
            outbox.setLeaseToken(token);
            outbox.setLeaseUntil(leaseUntil);
            return true;
        }).toList();
    }
}
