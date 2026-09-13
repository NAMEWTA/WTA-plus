package org.namewta.sso.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 第一方 SSO 组装配置。
 */
@Data
@ConfigurationProperties(prefix = "namewta.sso")
public class SsoProperties {

    /**
     * 是否组装 SSO 运行时。
     */
    private boolean enabled = true;

    /**
     * sso-web 独立 Origin。
     */
    private String webOrigin = "http://127.0.0.1:4176";

    /**
     * SSO 域会话 Cookie 名。
     */
    private String cookieName = "Sso-Token";

    /**
     * 授权码 TTL。
     */
    private Duration codeTtl = Duration.ofMinutes(5);

    /**
     * SSO 会话 TTL。
     */
    private Duration sessionTtl = Duration.ofHours(8);
}
