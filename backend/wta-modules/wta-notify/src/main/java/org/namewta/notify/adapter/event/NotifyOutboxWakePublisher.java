package org.namewta.notify.adapter.event;

import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.notify.support.outbox.NotifyOutboxWakeChannels;
import org.namewta.notify.support.outbox.NotifyOutboxWakeRequestedEvent;
import org.namewta.notify.support.outbox.NotifyOutboxWakeSignal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Outbox 提交后跨进程唤醒发布器。
 *
 * <p>Redis pub/sub 是唯一唤醒路径；提交线程最多等待发布确认 250ms，不调用 Worker。失败由慢轮询恢复。</p>
 */
@Slf4j
@Component
public class NotifyOutboxWakePublisher {

    private static final long PUBLISH_TIMEOUT_MILLIS = 250;
    private final NotifyOutboxWakeTransport transport;
    private final long timeoutMillis;

    /** 复用现有 RedisUtils 客户端的异步发布能力，不创建线程池或新的 Redis 客户端。 */
    public NotifyOutboxWakePublisher() {
        this(NotifyOutboxWakePublisher::publishViaRedis, PUBLISH_TIMEOUT_MILLIS);
    }

    /**
     * 模块内传输缝合点，生产等待上限固定为 250ms。
     * @param transport 返回异步发布结果的适配器
     * @param timeoutMillis 正数等待上限
     */
    NotifyOutboxWakePublisher(NotifyOutboxWakeTransport transport, long timeoutMillis) {
        if (timeoutMillis <= 0) throw new IllegalArgumentException("唤醒发布等待上限必须为正数");
        this.transport = transport;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 事务提交后有界发布跨进程唤醒；丢失或超时不回灌已提交事务。
     *
     * @param event 本事务写出可 claim Outbox 的登记
     */
    @DsTxEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(NotifyOutboxWakeRequestedEvent event) {
        if (event == null) {
            return;
        }
        NotifyOutboxWakeSignal signal = NotifyOutboxWakeSignal.wake(event.outboxId());
        Future<Long> pending = null;
        try {
            pending = transport.publish(NotifyOutboxWakeChannels.REDIS_CHANNEL, signal);
            pending.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException failure) {
            if (pending != null) pending.cancel(false);
            Thread.currentThread().interrupt();
            log.warn("notify outbox wake interrupted, outboxId={}, fallback poll remains", event.outboxId());
        } catch (ExecutionException | TimeoutException | RuntimeException failure) {
            if (pending != null) pending.cancel(false);
            log.warn("notify outbox cross-process wake publish failed, outboxId={}, reason={}, fallback poll remains",
                event.outboxId(), failure.getClass().getSimpleName());
        }
    }

    /**
     * 现有同步 publish 无独立等待上限，本事件适配器使用同一客户端的异步 Future。
     * @param channel 唤醒通道
     * @param signal 不含收件人、正文或凭据的载荷
     * @return 由现有 Redisson 事件循环完成的发布结果
     */
    private static Future<Long> publishViaRedis(String channel, NotifyOutboxWakeSignal signal) {
        return RedisUtils.getClient().getTopic(channel).publishAsync(signal);
    }

}

/**
 * 跨进程唤醒传输缝合点。生产实现复用 {@link RedisUtils#getClient()} 的异步发布。
 */
@FunctionalInterface
interface NotifyOutboxWakeTransport {

    /**
     * 向约定通道发布唤醒载荷。
     *
     * @param channel Redis 通道
     * @param signal  无 PII 唤醒载荷
     */
    Future<Long> publish(String channel, NotifyOutboxWakeSignal signal);
}
