package org.namewta.profile.person.port.provider;

/**
 * 个人认证提供方注册表端口。
 *
 * <p>业务 Service 只依赖该端口，具体 Spring 适配器位于 service.impl。</p>
 */
public interface PersonVerificationProviderRegistryPort {

    /** 按编码取得已启用的认证提供方。 */
    PersonVerificationProvider requireEnabled(String providerCode);
}
