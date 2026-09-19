package org.namewta.notify.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.port.NotifyDispatchResultPort;
import org.namewta.notify.service.runtime.NotifyDispatchResultService;
import org.springframework.stereotype.Service;

/** 由投递服务经 Spring 代理进入的结果事务，不包裹 Provider I/O。 */
@Service
@RequiredArgsConstructor
public class NotifyDispatchResultUseCase implements NotifyDispatchResultPort {
    private final NotifyDispatchResultService service;

    @Override
    @DSTransactional
    public boolean renew(NotifyOutbox lease) { return service.renew(lease); }

    @Override
    @DSTransactional
    public void complete(NotifyOutbox lease, Result result) { service.complete(lease, result); }

    @Override
    @DSTransactional
    public void settle(NotifyOutbox lease, Disposition disposition) { service.settle(lease, disposition); }

    @Override
    @DSTransactional
    public void refreshAggregate(Long intentId) { service.refreshAggregate(intentId); }
}
