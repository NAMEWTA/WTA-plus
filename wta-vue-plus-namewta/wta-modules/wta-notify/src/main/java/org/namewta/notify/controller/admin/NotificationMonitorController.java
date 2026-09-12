package org.namewta.notify.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.notify.api.NotificationSnapshot;
import org.namewta.notify.domain.vo.NotificationDeliveryView;
import org.namewta.notify.usecase.NotificationMonitorUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知统一监控接口，提供跨渠道的发送、接收人和供应商状态查询。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/notify/monitor")
public class NotificationMonitorController {

    private final NotificationMonitorUseCase monitorUseCase;

    /**
     * 查询通知详情。
     *
     * @param notificationId 通知编号
     * @return 通知快照
     */
    @SaCheckPermission("notify:monitor:query")
    @GetMapping("/snapshot")
    public R<NotificationSnapshot> snapshot(@RequestParam String notificationId) {
        return R.ok(monitorUseCase.snapshot(notificationId));
    }

    /**
     * 按接收人和渠道查询投递日志。
     *
     * @param userId 用户编号，可选
     * @param channel 渠道，可选
     * @param status 状态，可选
     * @return 投递日志
     */
    @SaCheckPermission("notify:monitor:list")
    @GetMapping("/deliveries")
    public R<List<NotificationDeliveryView>> deliveries(@RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) String channel,
                                              @RequestParam(required = false) String status) {
        return R.ok(monitorUseCase.deliveries(userId, channel, status));
    }
}
