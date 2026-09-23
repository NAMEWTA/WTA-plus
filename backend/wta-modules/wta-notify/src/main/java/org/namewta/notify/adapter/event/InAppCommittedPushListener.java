package org.namewta.notify.adapter.event;

import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.notify.port.InAppCommittedPushPort;
import org.namewta.notify.port.InAppDeliveryCommittedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;

/** 仅在动态数据源事务提交后发送站内刷新提示；推送失败不改变持久结果。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppCommittedPushListener {
    private final InAppCommittedPushPort port;

    /** 已提交主键是唯一事件载荷，失败不重写结果或记录正文。 */
    @DsTxEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(InAppDeliveryCommittedEvent event) {
        try {
            port.push(event.messageId(), event.userId());
        } catch (RuntimeException failure) {
            log.warn("站内实时提示失败，messageId={}, reason={}", event.messageId(), failure.getClass().getSimpleName());
        }
    }
}
