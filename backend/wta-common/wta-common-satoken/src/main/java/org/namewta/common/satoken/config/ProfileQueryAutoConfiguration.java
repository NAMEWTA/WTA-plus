package org.namewta.common.satoken.config;

import org.namewta.profile.api.CompositeProfileDisclosureService;
import org.namewta.profile.api.CompositeProfileService;
import org.namewta.profile.api.ProfileDisclosureContributor;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.profile.api.ProfileProjectionContributor;
import org.namewta.profile.api.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 中立的档案公共查询装配；个人、企业实现仅通过公开贡献者合同参与。
 *
 * <p>未接入某类贡献者时沿用公共 API 的未认证语义，不为装配而依赖任一业务实现模块。
 * 两个公共服务分别允许应用提供替代 Bean。</p>
 */
@AutoConfiguration
public class ProfileQueryAutoConfiguration {

    /** 创建非敏感摘要组合器，保留批量读取及缺失绑定语义。 */
    @Bean
    @ConditionalOnMissingBean(ProfileService.class)
    public ProfileService profileService(ObjectProvider<ProfileProjectionContributor> contributors) {
        return new CompositeProfileService(contributors.orderedStream().toList());
    }

    /** 创建按核准字段发布当前档案的组合器，不扩大非敏感摘要。 */
    @Bean
    @ConditionalOnMissingBean(ProfileDisclosureService.class)
    public ProfileDisclosureService profileDisclosureService(
        ObjectProvider<ProfileDisclosureContributor> contributors) {
        return new CompositeProfileDisclosureService(contributors.orderedStream().toList());
    }
}
