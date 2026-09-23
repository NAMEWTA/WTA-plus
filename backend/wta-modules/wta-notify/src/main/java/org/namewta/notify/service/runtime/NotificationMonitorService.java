package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;
import org.namewta.notify.domain.vo.NotificationDeliveryView;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知监控查询服务，统一提供按接收人、渠道和状态筛选的投递日志。
 */
@Service
@RequiredArgsConstructor
public class NotificationMonitorService {
    private final NotifyNotificationDao dao;

    /**
     * 查询最近投递记录。
     *
     * @param userId 用户编号，可选
     * @param channel 渠道，可选
     * @param status 状态，可选
     * @return 最多 500 条投递记录
     */
    public List<NotificationDeliveryView> listDeliveries(Long userId, String channel, String status) {
        List<NotifyDelivery> deliveries = dao.monitorDeliveries(userId, channel, status, 500);
        if (deliveries.isEmpty()) return List.of();
        Map<Long, NotifyIntent> intents = dao.intents(deliveries.stream().map(NotifyDelivery::getIntentId)
            .filter(java.util.Objects::nonNull).distinct().toList()).stream()
            .collect(Collectors.toMap(NotifyIntent::getIntentId, Function.identity()));
        return deliveries.stream().map(item -> NotificationDeliveryView.from(item, intents.get(item.getIntentId()))).toList();
    }
}
