package org.namewta.profile.person.usecase.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;
import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonVerificationCallbackOutcome;
import org.namewta.profile.person.port.verification.PersonVerificationService;
import org.namewta.profile.person.usecase.PersonVerificationUseCase;
import org.springframework.stereotype.Service;

/** 个人认证回调用例的实现，负责把入口请求交给认证业务服务。 */
@Service
@RequiredArgsConstructor
public class PersonVerificationUseCaseImpl implements PersonVerificationUseCase {

    private final PersonVerificationService coordinator;

    /**
     * 处理个人认证提供方回调。
     *
     * @param providerCode 认证提供方编码
     * @param envelope 提供方回调报文
     * @param receivedAt 接收时间
     * @return 回调处理结果
     */
    @DSTransactional
    @Override
    public PersonVerificationCallbackOutcome callback(String providerCode, PersonProviderCallbackEnvelope envelope,
                                                       java.time.Instant receivedAt) {
        return coordinator.handleCallback(providerCode, envelope, receivedAt);
    }
}
