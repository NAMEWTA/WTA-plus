package org.namewta.profile.enterprise.adapter.provider;
import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartCommand;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartResult;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationFailureCategory;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerifiedCallback;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProvider;
import org.springframework.stereotype.Component;
import java.time.Instant;
/** 企业人工认证提供方，创建待处理尝试并拒绝回调认证。 */
@Component
public class EnterpriseManualVerificationProvider implements EnterpriseVerificationProvider {
    /** 返回认证提供方编码。 */
    @Override
    public String providerCode() {
        return "manual";
    }
    /** 创建待处理的企业认证尝试。 */
    @Override
    public EnterpriseProviderStartResult start(EnterpriseProviderStartCommand command) {
        return EnterpriseProviderStartResult.pending();
    }
    /** 验证认证回调并返回规范化结果。 */
    @Override
    public EnterpriseVerifiedCallback authenticate(EnterpriseProviderCallbackEnvelope callback, Instant receivedAt) {
        throw new EnterpriseVerificationException(
            EnterpriseVerificationFailureCategory.UNSUPPORTED_CALLBACK,
            "Manual enterprise verification does not accept callbacks");
    }
}
