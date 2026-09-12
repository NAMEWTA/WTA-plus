package org.namewta.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.domain.R;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WebSocket 演示案例
 *
 * @author zendwang
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/websocket")
@Slf4j
public class WebSocketController {

    private final NotificationApplicationService notificationService;

    /**
     * 发布消息
     *
     * @param userId  目标用户
     * @param message 发送内容
     */
    @GetMapping("/send")
    public R<Void> send(Long userId, String message) {
        String recipientType = userId == null ? "ALL" : "USER";
        List<String> recipients = userId == null ? List.of() : List.of(String.valueOf(userId));
        notificationService.submit(new NotificationCommand("demo", "websocket-demo", "demo_websocket",
            String.valueOf(System.currentTimeMillis()), recipientType, recipients, "websocket-demo",
            java.util.Map.of("title", "实时消息", "content", message), List.of(NotificationChannel.IN_APP),
            NotificationStrategy.ALL, NotificationMode.ASYNC, 10, null, null, null, java.util.Map.of()));
        return R.ok("操作成功");
    }
}
