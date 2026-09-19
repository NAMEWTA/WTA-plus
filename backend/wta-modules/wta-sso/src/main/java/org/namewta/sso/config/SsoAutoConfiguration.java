package org.namewta.sso.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * 注册 SSO 配置属性。
 */
@Configuration
@EnableConfigurationProperties(SsoProperties.class)
public class SsoAutoConfiguration {
    /**
     * @param properties SSO运行配置
     * @param environment 实际激活的环境，不以构建文件名猜测生产边界
     */
    public SsoAutoConfiguration(SsoProperties properties, Environment environment) {
        if (properties.isEnabled() && !properties.isCookieSecure()) {
            String[] profiles = environment.getActiveProfiles();
            if (profiles.length == 0 || Arrays.stream(profiles).anyMatch(profile -> !"local".equals(profile) && !"dev".equals(profile))) {
                throw new IllegalArgumentException("Insecure SSO cookies require explicit local/dev profiles only");
            }
        }
    }
}
