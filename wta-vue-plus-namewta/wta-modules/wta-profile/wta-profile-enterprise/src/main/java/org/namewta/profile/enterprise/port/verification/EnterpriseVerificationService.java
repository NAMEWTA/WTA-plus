package org.namewta.profile.enterprise.port.verification;

import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationAttempt;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationCallbackOutcome;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationStartAttemptCommand;

import java.time.Instant;

/**
 * 承载EnterpriseVerificationService业务规则的领域服务。
 */
public interface EnterpriseVerificationService {

    /**
     * 启动认证尝试
     */
    EnterpriseVerificationAttempt startAttempt(EnterpriseVerificationStartAttemptCommand command);

    /**
     * 处理认证回调并更新尝试状态
     */
    EnterpriseVerificationCallbackOutcome handleCallback(String providerCode,
                                                          EnterpriseProviderCallbackEnvelope envelope,
                                                          Instant receivedAt);
}
