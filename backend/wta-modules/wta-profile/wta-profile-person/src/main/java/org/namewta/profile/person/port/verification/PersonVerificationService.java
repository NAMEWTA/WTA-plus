package org.namewta.profile.person.port.verification;

import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonVerificationAttempt;
import org.namewta.profile.person.domain.verification.PersonVerificationCallbackOutcome;
import org.namewta.profile.person.domain.verification.PersonVerificationStartAttemptCommand;

import java.time.Instant;

/**
 * 承载PersonVerificationService业务规则的领域服务。
 */
public interface PersonVerificationService {

    /**
     * 启动认证尝试
     */
    PersonVerificationAttempt startAttempt(PersonVerificationStartAttemptCommand command);

    /**
     * 处理认证回调并更新尝试状态
     */
    PersonVerificationCallbackOutcome handleCallback(String providerCode,
                                                      PersonProviderCallbackEnvelope envelope,
                                                      Instant receivedAt);
}
