package org.namewta.profile.person.adapter.provider;
import org.namewta.profile.person.port.provider.PersonVerificationProvider;
import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonProviderStartCommand;
import org.namewta.profile.person.domain.verification.PersonProviderStartResult;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.profile.person.domain.verification.PersonVerifiedCallback;
import org.springframework.stereotype.Component;
import java.time.Instant;
/** 个人人工认证提供方，创建待处理尝试并拒绝回调认证。 */
@Component
public class PersonManualVerificationProvider implements PersonVerificationProvider {
    /** 返回认证提供方编码。 */
    @Override
    public String providerCode() {
        return "manual";
    }
    /** 创建待处理的个人认证尝试。 */
    @Override
    public PersonProviderStartResult start(PersonProviderStartCommand command) {
        return PersonProviderStartResult.pending();
    }
    /** 验证认证回调并返回规范化结果。 */
    @Override
    public PersonVerifiedCallback authenticate(PersonProviderCallbackEnvelope callback, Instant receivedAt) {
        throw new PersonVerificationException(
            PersonVerificationFailureCategory.UNSUPPORTED_CALLBACK,
            "Manual person verification does not accept callbacks");
    }
}
