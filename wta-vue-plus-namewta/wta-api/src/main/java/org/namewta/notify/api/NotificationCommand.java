package org.namewta.notify.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 统一通知提交命令。
 *
 * @param appId 通知应用标识
 * @param sceneCode 业务场景编码
 * @param bizType 业务类型
 * @param bizId 业务主键
 * @param recipientType 接收者类型
 * @param recipientIds 接收者标识
 * @param templateCode 逻辑模板编码
 * @param templateParams 模板参数
 * @param channels 请求渠道
 * @param strategy 编排策略
 * @param mode 执行模式
 * @param priority 优先级
 * @param scheduledAt 计划时间
 * @param expiresAt 截止时间
 * @param idempotencyKey 幂等键
 * @param metadata 脱敏元数据
 */
public record NotificationCommand(String appId, String sceneCode, String bizType, String bizId,
                                  String recipientType, List<String> recipientIds, String templateCode,
                                  Map<String, Object> templateParams, List<NotificationChannel> channels,
                                  NotificationStrategy strategy, NotificationMode mode, int priority,
                                  Instant scheduledAt, Instant expiresAt, String idempotencyKey,
                                  Map<String, String> metadata) {
    public NotificationCommand {
        recipientIds = recipientIds == null ? List.of() : List.copyOf(recipientIds);
        templateParams = templateParams == null ? Map.of() : Map.copyOf(templateParams);
        channels = channels == null ? List.of() : List.copyOf(channels);
        strategy = strategy == null ? NotificationStrategy.ALL : strategy;
        mode = mode == null ? NotificationMode.ASYNC : mode;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
