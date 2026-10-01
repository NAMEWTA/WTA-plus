package org.namewta.sso.listener;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.usecase.SsoBusinessLogoutUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 全退后即使停止新认证，也继续完成第一方令牌注销。 */
@Component
@RequiredArgsConstructor
public class SsoBusinessLogoutListener {
    private final SsoBusinessLogoutUseCase useCase;
    private final org.namewta.sso.usecase.SsoSessionLifecycleUseCase lifecycle;

    /** 有界轮询恢复进程重启或Redis故障留下的注销任务。 */
    @Scheduled(fixedDelay = 2000, initialDelay = 2000)
    public void dispatch() {
        lifecycle.expire();
        useCase.dispatch();
    }
}
