package org.namewta.common.push.config;

import org.namewta.common.push.annotation.ConditionalOnMessageTransport;
import org.namewta.common.push.controller.SseController;
import org.namewta.common.push.core.SseEmitterSessionManager;
import org.namewta.common.push.listener.MessageTopicListener;
import org.namewta.common.push.properties.MessageProperties;
import org.namewta.common.push.security.PushTicketService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ScheduledExecutorService;

/**
 * SSE 消息推送自动装配。
 */
@AutoConfiguration(after = MessageAutoConfiguration.class)
@ConditionalOnMessageTransport("sse")
public class MessageSseConfiguration {

    /**
     * 注册 SSE 会话管理器
     * 负责管理用户 SSE 连接、消息发送、会话清理
     *
     * @return SseEmitterSessionManager 实例
     */
    @Bean
    public SseEmitterSessionManager sseEmitterManager(ScheduledExecutorService scheduledExecutorService,
                                                      MessageProperties messageProperties) {
        return new SseEmitterSessionManager(scheduledExecutorService, messageProperties);
    }

    /**
     * 注册消息主题监听器
     * 监听 Redis 全局消息，用于集群环境下的消息分发
     *
     * @param manager SSE 会话管理器
     * @return MessageTopicListener 实例
     */
    @Bean
    public MessageTopicListener messageTopicListener(SseEmitterSessionManager manager) {
        return new MessageTopicListener(manager);
    }

    /**
     * 注册 SSE 控制器
     * 提供前端建立 SSE 连接的接口
     *
     * @param manager SSE 会话管理器
     * @return SseController 实例
     */
    @Bean
    public SseController sseController(SseEmitterSessionManager manager, PushTicketService ticketService) {
        return new SseController(manager, ticketService);
    }
}
