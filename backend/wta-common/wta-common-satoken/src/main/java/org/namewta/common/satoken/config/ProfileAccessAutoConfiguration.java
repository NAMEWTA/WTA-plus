package org.namewta.common.satoken.config;

import org.namewta.common.satoken.profile.ProfileAccess;
import org.namewta.common.satoken.profile.ProfileVerificationAdvisor;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.profile.api.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

/** 当前账户档案入口及认证注解的自动装配，不改变既有接口的认证要求。 */
@AutoConfiguration(after = {SaTokenConfig.class, ProfileQueryAutoConfiguration.class})
public class ProfileAccessAutoConfiguration {

    /** 共用一个访问组件，保证注解、注入调用和静态入口的身份与错误语义一致。 */
    @Bean
    @ConditionalOnMissingBean(ProfileAccess.class)
    public ProfileAccess profileAccess(ProfileService profiles, ProfileDisclosureService disclosures) {
        return new ProfileAccess(profiles, disclosures);
    }

    /**
     * Advisor 只持有延迟提供者，避免创建代理时提前实例化档案贡献者及业务服务。
     *
     * @param access 仅在命中受保护方法时解析的档案访问组件
     * @return 统一认证拦截器
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(ProfileVerificationAdvisor.class)
    public static ProfileVerificationAdvisor profileVerificationAdvisor(ObjectProvider<ProfileAccess> access) {
        return new ProfileVerificationAdvisor(access);
    }
}
