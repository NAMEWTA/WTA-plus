package org.namewta.profile.enterprise.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationCallbackOutcome;
import org.namewta.profile.enterprise.port.verification.EnterpriseVerificationService;
import org.namewta.profile.enterprise.usecase.EnterpriseVerificationUseCase;
import org.springframework.stereotype.Service;

/**
 * EnterpriseVerificationUseCaseImpl 应用用例合同，定义入口可调用的业务场景。
 */
@Service
@RequiredArgsConstructor
public class EnterpriseVerificationUseCaseImpl implements EnterpriseVerificationUseCase {
    private final EnterpriseVerificationService coordinator;
    /** 处理企业认证提供方回调。 */
    @DSTransactional
    @Override
    public EnterpriseVerificationCallbackOutcome callback(String providerCode,
                                                           EnterpriseProviderCallbackEnvelope envelope,
                                                           java.time.Instant receivedAt) {
        return coordinator.handleCallback(providerCode, envelope, receivedAt);
    }
}
