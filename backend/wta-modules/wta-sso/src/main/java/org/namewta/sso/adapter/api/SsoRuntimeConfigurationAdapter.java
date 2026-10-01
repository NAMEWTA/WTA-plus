package org.namewta.sso.adapter.api;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.api.SsoRuntimeConfiguration;
import org.namewta.sso.api.SsoRuntimeSettings;
import org.namewta.sso.usecase.SsoConfigurationUseCase;
import org.springframework.stereotype.Component;

/** 中央配置公共API，禁止OIDC直接访问SSO表。 */
@Component
@RequiredArgsConstructor
public class SsoRuntimeConfigurationAdapter implements SsoRuntimeConfiguration {
    private final SsoConfigurationUseCase useCase;

    @Override
    public SsoRuntimeSettings current() {
        return useCase.current();
    }

    @Override
    public SsoRuntimeSettings saved() {
        return useCase.saved();
    }

    @Override
    public void save(SsoRuntimeSettings settings) {
        useCase.save(settings);
    }
}
