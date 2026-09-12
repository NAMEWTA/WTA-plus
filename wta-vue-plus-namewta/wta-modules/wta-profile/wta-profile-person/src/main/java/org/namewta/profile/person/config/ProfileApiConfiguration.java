package org.namewta.profile.person.config;

import org.namewta.profile.api.CompositeProfileService;
import org.namewta.profile.api.ProfileProjectionContributor;
import org.namewta.profile.api.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 装配跨模块档案查询门面。
 */
@Configuration(proxyBeanMethods = false)
public class ProfileApiConfiguration {

    /** 创建档案服务组合器。 */
    @Bean
    @ConditionalOnMissingBean(ProfileService.class)
    public ProfileService profileService(ObjectProvider<ProfileProjectionContributor> contributors) {
        return new CompositeProfileService(contributors.orderedStream().toList());
    }
}
