package org.namewta.notify.service.runtime;

import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.notify.support.outbox.NotifyOutboxWakeChannels;
import org.namewta.notify.support.outbox.NotifyOutboxWakeRequestedEvent;
import org.namewta.notify.support.outbox.NotifyOutboxWakeSignal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Outbox 提交后跨进程唤醒发布器。
 *
 * <p>Redis pub/sub 是多实例主路径；同 JVM 事件仅为辅信号。发布失败只记日志，不得回灌已提交事务。</p>
 */
@Slf4j
@Component
public class NotifyOutboxWakePublisher {

    private final NotifyOutboxWakeTransport transport;
    private final ApplicationEventPublisher localEvents;

    /**
     * 生产构造：经 {@link RedisUtils} 发布跨进程信号。
     *
     * @param localEvents 同 JVM 辅信号出口
     */
    @Autowired
    public NotifyOutboxWakePublisher(ApplicationEventPublisher localEvents) {
        this(NotifyOutboxWakePublisher::publishViaRedis, localEvents);
    }

    /**
     * 测试缝合点：替换跨进程传输而不加载 Redis 客户端。
     *
     * @param transport   跨进程发布适配
     * @param localEvents 同 JVM 辅信号出口
     */
    NotifyOutboxWakePublisher(NotifyOutboxWakeTransport transport, ApplicationEventPublisher localEvents) {
        this.transport = transport;
        this.localEvents = localEvents;
    }

    /**
     * 事务提交后发布跨进程唤醒，并可选发出同 JVM 辅信号。
     *
     * @param event 本事务写出可 claim Outbox 的登记
     */
    @DsTxEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(NotifyOutboxWakeRequestedEvent event) {
        if (event == null) {
            return;
        }
        NotifyOutboxWakeSignal signal = NotifyOutboxWakeSignal.wake(event.outboxId());
        try {
            transport.publish(NotifyOutboxWakeChannels.REDIS_CHANNEL, signal);
        } catch (RuntimeException ex) {
            log.warn("notify outbox cross-process wake publish failed, outboxId={}, fallback poll remains",
                event.outboxId(), ex);
        }
        try {
            localEvents.publishEvent(signal);
        } catch (RuntimeException ex) {
            log.warn("notify outbox local wake signal failed, outboxId={}", event.outboxId(), ex);
        }
    }

    private static void publishViaRedis(String channel, NotifyOutboxWakeSignal signal) {
        RedisUtils.publish(channel, signal);
    }
}

/**
 * 跨进程唤醒传输缝合点。生产实现调用 {@link RedisUtils#publish(String, Object)}。
 */
@FunctionalInterface
interface NotifyOutboxWakeTransport {

    /**
     * 向约定通道发布唤醒载荷。
     *
     * @param channel Redis 通道
     * @param signal  无 PII 唤醒载荷
     */
    void publish(String channel, NotifyOutboxWakeSignal signal);
}
