package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.ProviderCallbackPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 供应商回执用例，负责幂等更新投递状态并刷新通知聚合状态。
 */
@Service
@RequiredArgsConstructor
public class ProviderCallbackService implements ProviderCallbackPort {
    private static final Set<String> CALLBACK_STATUSES = Set.of("ACCEPTED", "DELIVERED", "UNDELIVERABLE", "FAILED", "UNKNOWN");

    private final NotifyNotificationDao dao;
    private final NotifyDispatchPort dispatchService;
    private final Map<String, Long> seenEvents = new ConcurrentHashMap<>();

    /** 应用供应商回执；旧状态等级高于回执时忽略乱序消息。 */
    @Override
    public void apply(String channel, String providerKey, String providerMessageId, String status, String eventId) {
        if (channel == null || channel.isBlank() || providerMessageId == null || providerMessageId.isBlank()
            || status == null || status.isBlank()) {
            throw new ServiceException("回调渠道、消息编号和状态不能为空");
        }
        if (providerKey == null || providerKey.isBlank() || eventId == null || eventId.isBlank()) {
            throw new ServiceException("回调供应商和事件 ID 不能为空");
        }
        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        if (!CALLBACK_STATUSES.contains(normalizedStatus)) throw new ServiceException("回调状态不支持");
        long now = System.currentTimeMillis();
        seenEvents.entrySet().removeIf(entry -> entry.getValue() < now - 10 * 60_000L);
        if (seenEvents.containsKey(eventId)) return;
        NotifyDelivery delivery = dao.deliveryByProvider(channel.toUpperCase(Locale.ROOT), providerKey, providerMessageId);
        if (delivery == null) throw new ServiceException("未找到对应投递记录");
        if (!isAllowedTransition(delivery.getStatus(), normalizedStatus)) {
            seenEvents.putIfAbsent(eventId, now);
            return;
        }
        String previousStatus = delivery.getStatus();
        delivery.setStatus(normalizedStatus);
        if ("ACCEPTED".equals(normalizedStatus)) delivery.setAcceptedAt(LocalDateTime.now());
        if ("DELIVERED".equals(normalizedStatus)) {
            delivery.setAcceptedAt(delivery.getAcceptedAt() == null ? LocalDateTime.now() : delivery.getAcceptedAt());
            delivery.setDeliveredAt(LocalDateTime.now());
        }
        int updated = dao.updateDeliveryStatus(delivery.getDeliveryId(), previousStatus, delivery);
        if (updated == 0) return;
        seenEvents.putIfAbsent(eventId, now);
        dispatchService.refreshAggregate(delivery.getIntentId());
    }

    private int rank(String status) {
        if (status == null) return 0;
        return switch (status) {
            case "DELIVERED" -> 4;
            case "UNDELIVERABLE", "FAILED" -> 3;
            case "ACCEPTED" -> 2;
            default -> 1;
        };
    }

    private boolean isAllowedTransition(String current, String next) {
        if (current == null || current.isBlank()) return true;
        if (Set.of("DELIVERED", "CANCELLED", "FAILED", "UNDELIVERABLE").contains(current)) return false;
        return rank(next) > rank(current);
    }
}

