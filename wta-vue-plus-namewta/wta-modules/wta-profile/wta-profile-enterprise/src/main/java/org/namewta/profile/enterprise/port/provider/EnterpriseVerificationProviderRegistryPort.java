package org.namewta.profile.enterprise.port.provider;

/**
 * 企业认证提供方注册表端口。
 *
 * <p>业务 Service 只依赖该端口，具体 Spring 适配器位于 service.impl。</p>
 */
public interface EnterpriseVerificationProviderRegistryPort {

    /** 按编码取得已启用的认证提供方。 */
    EnterpriseVerificationProvider requireEnabled(String providerCode);
}
