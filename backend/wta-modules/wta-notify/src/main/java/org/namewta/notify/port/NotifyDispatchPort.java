package org.namewta.notify.port;

import org.namewta.notify.domain.entity.NotifyOutbox;

/** Outbox 投递端口，由运行时服务实现，Worker 只依赖此合同。 */
public interface NotifyDispatchPort {
    void dispatch(NotifyOutbox outbox);
    void refreshAggregate(Long intentId);
}
