package org.namewta.notify.service.runtime;

import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<NotifyDelivery> listDeliveries(Long userId, String channel, String status) {
        return dao.monitorDeliveries(userId, channel, status, 500);
    }
}

