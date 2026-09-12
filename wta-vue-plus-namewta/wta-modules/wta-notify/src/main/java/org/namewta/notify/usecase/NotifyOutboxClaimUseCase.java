package org.namewta.notify.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyOutbox;
import org.namewta.notify.port.NotifyOutboxClaimPort;
import org.namewta.notify.service.runtime.NotifyOutboxClaimService;
import org.springframework.stereotype.Service;

import java.util.List;

/** Outbox 领取用例，保证租约建立发生在独立短事务内。 */
@Service
@RequiredArgsConstructor
public class NotifyOutboxClaimUseCase implements NotifyOutboxClaimPort {
    private final NotifyOutboxClaimService claimService;

    @Override
    @DSTransactional
    public List<NotifyOutbox> claim(String owner) {
        return claimService.claim(owner);
    }
}
