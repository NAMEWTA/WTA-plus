package org.namewta.notify.usecase;

import lombok.RequiredArgsConstructor;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationQuery;
import org.namewta.notify.api.NotificationSnapshot;
import org.namewta.notify.domain.vo.NotificationDeliveryView;
import org.namewta.notify.service.runtime.NotificationMonitorService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 通知监控查询用例。 */
@Service
@RequiredArgsConstructor
public class NotificationMonitorUseCase {
    private final NotificationApplicationService notificationService;
    private final NotificationMonitorService monitorService;

    public NotificationSnapshot snapshot(String id) { return notificationService.query(new NotificationQuery(id, false)); }
    public List<NotificationDeliveryView> deliveries(Long userId, String channel, String status) {
        return monitorService.listDeliveries(userId, channel, status).stream().map(NotificationDeliveryView::from).toList();
    }
}

