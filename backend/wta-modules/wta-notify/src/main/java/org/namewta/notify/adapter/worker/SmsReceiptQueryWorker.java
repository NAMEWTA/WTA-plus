package org.namewta.notify.adapter.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.notify.port.SmsReceiptQueryPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;

/** 每轮最多核对一条；Spring拥有调度生命周期，不另建线程或无限drain。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsReceiptQueryWorker {
    private final SmsReceiptQueryPort useCase;
    private final AtomicBoolean running = new AtomicBoolean();

    /** 默认每5秒选一条到期记录；同一条15分钟才再次预约，状态查询不触发发送。 */
    @Scheduled(fixedDelay = 5000, initialDelay = 60000)
    public void poll() {
        if (!running.compareAndSet(false, true)) return;
        Long deliveryId = null;
        try {
            var delivery = useCase.claim();
            if (delivery == null) return;
            deliveryId = delivery.getDeliveryId();
            useCase.query(delivery);
        } catch (RuntimeException exception) {
            // 供应商异常cause可含凭据、号码或短信原文；日志只保留投递主键与固定说明。
            log.warn("notify sms receipt query deferred deliveryId={} reason=QUERY_UNCONFIRMED", deliveryId);
        } finally {
            running.set(false);
        }
    }
}
