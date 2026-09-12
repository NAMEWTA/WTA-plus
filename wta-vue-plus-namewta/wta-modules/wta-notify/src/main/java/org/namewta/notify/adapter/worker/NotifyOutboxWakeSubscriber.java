package org.namewta.notify.adapter.worker;

import lombok.extern.slf4j.Slf4j;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.notify.support.outbox.NotifyOutboxWakeChannels;
import org.namewta.notify.support.outbox.NotifyOutboxWakeSignal;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 订阅 T-01 Redis 唤醒通道，转发到 Worker 统一 claim+dispatch 循环。
 *
 * <p>订阅失败不得阻止进程启动；空闲投递仍依赖慢速兜底。</p>
 */
@Slf4j
@Component
public class NotifyOutboxWakeSubscriber implements InitializingBean, DisposableBean {

    private final NotifyOutboxWorker worker;
    private final NotifyOutboxWakeBus bus;
    private Integer listenerId;

    /**
     * 生产构造：经 {@link RedisUtils} 订阅跨进程通道。
     *
     * @param worker 统一领取循环
     */
    @Autowired
    public NotifyOutboxWakeSubscriber(NotifyOutboxWorker worker) {
        this(worker, redisBus());
    }

    /**
     * 测试缝合点：替换 Redis 订阅而不加载客户端。
     *
     * @param worker 统一领取循环
     * @param bus    订阅适配
     */
    NotifyOutboxWakeSubscriber(NotifyOutboxWorker worker, NotifyOutboxWakeBus bus) {
        this.worker = worker;
        this.bus = bus;
    }

    /**
     * 组件启动时订阅唤醒通道。
     */
    @Override
    public void afterPropertiesSet() {
        try {
            listenerId = bus.subscribe(
                NotifyOutboxWakeChannels.REDIS_CHANNEL,
                NotifyOutboxWakeSignal.class,
                this::onMessage);
        } catch (RuntimeException ex) {
            log.warn("notify outbox wake subscribe failed, slow poll remains", ex);
        }
    }

    /**
     * 组件销毁时取消订阅。
     */
    @Override
    public void destroy() {
        if (listenerId == null) {
            return;
        }
        try {
            bus.unsubscribe(NotifyOutboxWakeChannels.REDIS_CHANNEL, listenerId);
        } catch (RuntimeException ex) {
            log.warn("notify outbox wake unsubscribe failed", ex);
        }
    }

    private void onMessage(NotifyOutboxWakeSignal signal) {
        try {
            worker.onWake(signal);
        } catch (RuntimeException ex) {
            log.warn("notify outbox wake drain failed, trigger=WAKE", ex);
        }
    }

    private static NotifyOutboxWakeBus redisBus() {
        return new NotifyOutboxWakeBus() {
            @Override
            public int subscribe(String channel, Class<NotifyOutboxWakeSignal> type,
                                 Consumer<NotifyOutboxWakeSignal> consumer) {
                return RedisUtils.subscribeAndGetListenerId(channel, type, consumer);
            }

            @Override
            public void unsubscribe(String channel, int listenerId) {
                RedisUtils.unsubscribe(channel, listenerId);
            }
        };
    }
}

/**
 * Outbox 唤醒总线。生产实现调用 {@link RedisUtils} 的 subscribe/unsubscribe。
 */
interface NotifyOutboxWakeBus {

    /**
     * 订阅通道并返回监听器 ID。
     *
     * @param channel  通道
     * @param type     载荷类型
     * @param consumer 回调
     * @return 监听器 ID
     */
    int subscribe(String channel, Class<NotifyOutboxWakeSignal> type, Consumer<NotifyOutboxWakeSignal> consumer);

    /**
     * 取消订阅。
     *
     * @param channel    通道
     * @param listenerId 监听器 ID
     */
    void unsubscribe(String channel, int listenerId);
}
