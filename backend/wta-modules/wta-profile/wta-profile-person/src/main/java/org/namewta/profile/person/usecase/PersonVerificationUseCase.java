package org.namewta.profile.person.usecase;

import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonVerificationCallbackOutcome;

import java.time.Instant;

/**
 * PersonVerificationUseCase 应用用例合同，定义入口可调用的业务场景。
 */
public interface PersonVerificationUseCase {

    /**
     * 编排 callback 应用用例。
     */
    PersonVerificationCallbackOutcome callback(String providerCode,
                                               PersonProviderCallbackEnvelope envelope,
                                               Instant receivedAt);
}
