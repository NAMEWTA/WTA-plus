package org.namewta.sso.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/** 注册 SSO 配置属性。 */
@Configuration
public class SsoAutoConfiguration {
    /** Spring通过无参构造器装配数据库配置Bean。 */
    public SsoAutoConfiguration() {}

    /** 保留旧Java配置校验构造器；生产属性仍只由数据库Bean供应。 */
    public SsoAutoConfiguration(SsoProperties properties, Environment environment) {
        validate(properties, environment);
    }

    /** Cookie传输边界也用于旧显式装配。 */
    private static void validate(SsoProperties properties, Environment environment) {
        if (properties.isEnabled() && !properties.isCookieSecure()) {
            String[] profiles = environment.getActiveProfiles();
            if (profiles.length == 0
                    || Arrays.stream(profiles)
                            .anyMatch(p -> !"local".equals(p) && !"dev".equals(p)))
                throw new IllegalArgumentException(
                        "Insecure SSO cookies require explicit local/dev profiles only");
        }
    }

    /**
     * @param properties SSO运行配置
     * @param environment 实际激活的环境，不以构建文件名猜测生产边界
     */
    @org.springframework.context.annotation.Bean
    public SsoProperties ssoProperties(
            org.namewta.sso.service.SsoConfigurationService service, Environment environment) {
        var properties = new SsoProperties();
        properties.bind(service::current);
        if (properties.isEnabled() && !properties.isCookieSecure()) {
            String[] profiles = environment.getActiveProfiles();
            if (profiles.length == 0
                    || Arrays.stream(profiles)
                            .anyMatch(
                                    profile ->
                                            !"local".equals(profile) && !"dev".equals(profile))) {
                throw new IllegalArgumentException(
                        "Insecure SSO cookies require explicit local/dev profiles only");
            }
        }
        return properties;
    }
}
