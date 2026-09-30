package org.namewta.oidc.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/** 独立提交一次消费，后续签发失败不能恢复凭据。 */
@Component
@RequiredArgsConstructor
public class OidcConsumeUseCase {
    private final OidcAuthorizationWorkflow service;

    @DSTransactional
    /** 原子消费未过期的一次性凭据，失败时不得恢复可用状态。 */
    public boolean consume(String code, String clientId) {
        return service.consume(code, clientId);
    }
}
