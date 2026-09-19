package org.namewta.notify.adapter.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.port.NotifyDispatchPort;
import org.namewta.notify.port.NotifyOutboxClaimPort;
import org.namewta.notify.support.outbox.NotifyOutboxWakeSignal;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Outbox Worker：跨进程唤醒与慢速兜底共用 claim+dispatch，不按 outboxId 直投。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyOutboxWorker {

    private final NotifyOutboxClaimPort claimService;
    private final NotifyDispatchPort dispatchService;
    private final String owner = UUID.randomUUID().toString();
    private final AtomicBoolean draining = new AtomicBoolean();

    /**
     * 慢速兜底扫描。默认 60000ms，配置项名保持 {@code notify.outbox.poll-delay-ms}。
     */
    @Scheduled(fixedDelayString = "${notify.outbox.poll-delay-ms:60000}")
    public void poll() {
        drain(Trigger.POLL);
    }

    /**
     * Redis 跨进程唤醒入口。载荷中的 outboxId 只是 hint，必须再走 claim/lease。
     *
     * @param signal T-01 唤醒信号，允许为 {@code null}
     */
    public void onWake(NotifyOutboxWakeSignal signal) {
        drain(Trigger.WAKE);
    }

    /**
     * 连续领取直到本批为空。{@code trigger} 写入日志以区分唤醒与兜底。
     *
     * @param trigger 唤醒或定时兜底
     */
    void drain(Trigger trigger) {
        // 重叠信号不排队、不创建线程；最后一次空批与新信号相撞时由既有慢轮询兜底。
        if (!draining.compareAndSet(false, true)) return;
        try {
            int claimed = 0;
            int batches = 0;
            List<NotifyOutbox> batch;
            do {
                batch = claimService.claim(owner);
                for (NotifyOutbox outbox : batch) dispatchService.dispatch(outbox);
                claimed += batch.size();
                batches++;
            } while (!batch.isEmpty());
            log.info("notify outbox drain trigger={} claimed={} batches={}", trigger, claimed, batches);
        } finally {
            draining.set(false);
        }
    }

    /**
     * 领取触发来源，供日志区分唤醒与兜底 tick。
     */
    enum Trigger {
        /** 跨进程或同 JVM 唤醒。 */
        WAKE,
        /** {@code @Scheduled} 慢速兜底。 */
        POLL
    }
}
