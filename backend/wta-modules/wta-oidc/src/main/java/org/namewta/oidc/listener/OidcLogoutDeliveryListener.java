package org.namewta.oidc.listener;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.usecase.OidcLogoutUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 停止新签发不停止已有退出任务。 */
@Component
@RequiredArgsConstructor
public class OidcLogoutDeliveryListener {
    private final OidcLogoutUseCase useCase;

    /** 重启后从MySQL恢复到期投递。 */
    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    public void dispatch() {
        useCase.dispatch();
    }
}
