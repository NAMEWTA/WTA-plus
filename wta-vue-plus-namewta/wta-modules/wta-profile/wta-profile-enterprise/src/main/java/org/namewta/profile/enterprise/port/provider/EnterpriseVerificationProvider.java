package org.namewta.profile.enterprise.port.provider;

import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartCommand;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartResult;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerifiedCallback;
import java.time.Instant;

/**
 * 企业认证提供方适配器契约，负责认证并返回规范化证据，不直接发布档案或变更绑定。
 *
 * Adapter contract for one enterprise-verification provider.
 *
 * <p>The adapter authenticates provider-specific callbacks and returns only
 * normalized evidence. It must never publish a profile or change a binding.</p>
 */
public interface EnterpriseVerificationProvider {

    /** 返回认证提供方编码。 */
    String providerCode();

    /** 启动认证或工作流流程。 */
    EnterpriseProviderStartResult start(EnterpriseProviderStartCommand command);

    /** 验证认证回调并返回规范化结果。 */
    EnterpriseVerifiedCallback authenticate(EnterpriseProviderCallbackEnvelope callback, Instant receivedAt);
}
