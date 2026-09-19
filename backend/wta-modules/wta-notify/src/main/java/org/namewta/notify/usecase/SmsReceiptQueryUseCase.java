package org.namewta.notify.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.port.SmsReceiptQueryPort;
import org.namewta.notify.service.runtime.SmsReceiptQueryService;
import org.springframework.stereotype.Service;

/** 预约在短动态事务中提交，查询通过另一公开入口执行，避免自调用事务。 */
@Service
@RequiredArgsConstructor
public class SmsReceiptQueryUseCase implements SmsReceiptQueryPort {
    private final SmsReceiptQueryService service;

    @Override
    @DSTransactional
    public NotifyDelivery claim() { return service.claim(); }

    @Override
    public void query(NotifyDelivery delivery) { service.query(delivery); }
}
