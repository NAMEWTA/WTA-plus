package org.namewta.sso.config;

import org.namewta.sso.dao.SsoAuthorizationCodeDao;
import org.namewta.sso.port.SsoBusinessTokenPort;
import org.namewta.sso.port.SsoClientCatalogPort;
import org.namewta.sso.port.SsoIdentityPort;
import org.namewta.sso.service.SsoAuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * SSO 服务时钟与授权引擎。
 */
@Configuration
public class SsoServiceConfiguration {

    /**
     * 系统时钟。
     *
     * @return 时钟
     */
    @Bean
    public Clock ssoClock() {
        return Clock.systemDefaultZone();
    }

    /**
     * 授权引擎。
     *
     * @param codeDao       授权码 DAO
     * @param clientCatalog 目录
     * @param identityPort  认人
     * @param tokenPort     令牌
     * @param clock         时钟
     * @param properties    配置
     * @return 授权服务
     */
    @Bean
    public SsoAuthorizationService ssoAuthorizationService(SsoAuthorizationCodeDao codeDao,
                                                           SsoClientCatalogPort clientCatalog,
                                                           SsoIdentityPort identityPort,
                                                           SsoBusinessTokenPort tokenPort,
                                                           Clock clock,
                                                           SsoProperties properties) {
        return new SsoAuthorizationService(codeDao, clientCatalog, identityPort, tokenPort, clock, properties.getCodeTtl());
    }
}
