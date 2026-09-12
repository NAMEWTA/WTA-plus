package org.namewta.notify.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.notify.api.CancelReceipt;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCancelCommand;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationQuery;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationRetryCommand;
import org.namewta.notify.api.NotificationSnapshot;
import org.namewta.notify.api.RetryReceipt;
import org.namewta.notify.service.runtime.NotificationApplicationRuntimeService;
import org.springframework.stereotype.Service;

/** 统一通知应用用例，集中承载事务和 public API 委托。 */
@Service
@RequiredArgsConstructor
public class NotificationApplicationUseCase implements NotificationApplicationService {
    private final NotificationApplicationRuntimeService runtimeService;

    @Override
    @DSTransactional
    public NotificationReceipt submit(NotificationCommand command) {
        return runtimeService.submit(command);
    }

    @Override
    public NotificationSnapshot query(NotificationQuery query) {
        return runtimeService.query(query);
    }

    @Override
    @DSTransactional
    public RetryReceipt retry(NotificationRetryCommand command) {
        return runtimeService.retry(command);
    }

    @Override
    @DSTransactional
    public CancelReceipt cancel(NotificationCancelCommand command) {
        return runtimeService.cancel(command);
    }
}
