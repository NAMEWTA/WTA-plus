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

    /** sso-web 的 Vite/router base；独立 Origin 不包含该路径。 */
    private String webBasePath = "/";

    /**
     * 仅接受根路径或发布清单中的单层前缀，防止配置改变授权地址的 Origin。
     *
     * @param webBasePath 带首尾斜杠的静态页面 base
     */
    public void setWebBasePath(String webBasePath) {
        if (webBasePath == null || !webBasePath.matches("/(?:[A-Za-z0-9][A-Za-z0-9-]*/)?")) {
            throw new IllegalArgumentException("SSO web base path must be / or /prefix/");
        }
        this.webBasePath = webBasePath;
    }

    /**
     * SSO 域会话 Cookie 名。
     */
    private String cookieName = "Sso-Token";

    /** 默认HTTPS Cookie；显式关闭仅允许已声明的local/dev环境。 */
    private boolean cookieSecure = true;

    /**
     * 授权码 TTL。
     */
    private Duration codeTtl = Duration.ofMinutes(5);

    /**
     * SSO 会话 TTL。
     */
    private Duration sessionTtl = Duration.ofHours(8);
}
