package org.namewta.profile.person.port.provider;

import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonProviderStartCommand;
import org.namewta.profile.person.domain.verification.PersonProviderStartResult;
import org.namewta.profile.person.domain.verification.PersonVerifiedCallback;
import java.time.Instant;

/**
 * 个人认证提供方适配器契约，负责认证并返回规范化证据，不直接发布档案或变更绑定。
 *
 * Adapter contract for one person-verification provider.
 *
 * <p>The adapter authenticates provider-specific callbacks and returns only
 * normalized evidence. It must never publish a profile or change a binding.</p>
 */
public interface PersonVerificationProvider {

    /** 返回认证提供方编码。 */
    String providerCode();

    /** 启动认证或工作流流程。 */
    PersonProviderStartResult start(PersonProviderStartCommand command);

    /** 验证认证回调并返回规范化结果。 */
    PersonVerifiedCallback authenticate(PersonProviderCallbackEnvelope callback, Instant receivedAt);
}
