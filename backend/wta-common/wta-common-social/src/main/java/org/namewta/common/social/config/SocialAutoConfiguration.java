package org.namewta.common.social.config;

import me.zhyd.oauth.cache.AuthStateCache;
import org.namewta.common.social.utils.AuthRedisStateCache;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Social 配置属性
 */
@AutoConfiguration
public class SocialAutoConfiguration {

    /** 根密钥仅从启动配置注入，业务数据库和浏览器均不持有此密钥。 */
    @Bean
    public SocialSecretCipher socialSecretCipher(
        @Value("${authentication.secrets.root-key:${AUTH_CONFIG_ROOT_KEY:}}") String rootKey) {
        return new SocialSecretCipher(rootKey);
    }

    /**
     * 注册第三方授权状态缓存实现。
     *
     * @return 授权状态缓存
     */
    @Bean
    public AuthStateCache authStateCache() {
        return new AuthRedisStateCache();
    }

}
