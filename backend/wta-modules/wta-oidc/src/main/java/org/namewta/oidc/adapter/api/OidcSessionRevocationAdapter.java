package org.namewta.oidc.adapter.api;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.usecase.OidcLogoutUseCase;
import org.namewta.sso.api.SsoSessionRevocationParticipant;
import org.springframework.stereotype.Component;

/** 同步加入中央撤销事务，不使用可能丢失的提交后内存事件预约退出。 */
@Component
@RequiredArgsConstructor
public class OidcSessionRevocationAdapter implements SsoSessionRevocationParticipant {
    private final OidcLogoutUseCase useCase;

    @Override
    public void revoke(String hash) {
        useCase.revoke(hash);
    }
}
