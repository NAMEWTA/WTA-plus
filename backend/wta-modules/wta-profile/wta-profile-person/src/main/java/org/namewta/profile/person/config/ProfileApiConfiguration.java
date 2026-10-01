package org.namewta.profile.person.config;

import org.namewta.common.satoken.config.ProfileQueryAutoConfiguration;
import org.namewta.profile.api.CompositeProfileService;
import org.namewta.profile.api.ProfileProjectionContributor;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.CompositeProfileDisclosureService;
import org.namewta.profile.api.ProfileDisclosureContributor;
import org.namewta.profile.api.ProfileDisclosureService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 档案查询装配的兼容入口；真实 Bean 由中立配置创建，不再依附个人模块。
 * <p>延迟导入以先注册应用自定义 Bean，再判断是否提供默认组合器。</p>
 */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(ProfileQueryAutoConfiguration.class)
public class ProfileApiConfiguration {

    /**
     * 保留直接调用旧工厂方法的兼容性，不再注册重复 Bean。
     *
     * @param contributors 已接入的字段贡献者
     * @return 按核准字段发布档案的组合器
     * @deprecated 不建议新代码优先使用；应用注入 {@link ProfileDisclosureService}，
     *     或使用 {@link ProfileQueryAutoConfiguration} 完成装配。
     */
    @Deprecated(since = "6.0.0", forRemoval = false)
    public ProfileDisclosureService profileDisclosureService(ObjectProvider<ProfileDisclosureContributor> contributors) {
        return new CompositeProfileDisclosureService(contributors.orderedStream().toList());
    }

    /**
     * 保留直接调用旧工厂方法的兼容性，不再注册重复 Bean。
     *
     * @param contributors 已接入的批量摘要贡献者
     * @return 非敏感档案摘要组合器
     * @deprecated 不建议新代码优先使用；应用注入 {@link ProfileService}，
     *     或使用 {@link ProfileQueryAutoConfiguration} 完成装配。
     */
    @Deprecated(since = "6.0.0", forRemoval = false)
    public ProfileService profileService(ObjectProvider<ProfileProjectionContributor> contributors) {
        return new CompositeProfileService(contributors.orderedStream().toList());
    }
}
