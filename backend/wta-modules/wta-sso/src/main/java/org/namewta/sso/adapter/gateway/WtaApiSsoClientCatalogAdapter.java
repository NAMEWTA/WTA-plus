package org.namewta.sso.adapter.gateway;

import lombok.RequiredArgsConstructor;
import org.namewta.sso.api.SsoClientCatalog;
import org.namewta.sso.api.SsoClientView;
import org.namewta.sso.port.SsoClientCatalogPort;
import org.springframework.stereotype.Component;

/**
 * 经 wta-api 读取 Client 目录。
 */
@Component
@RequiredArgsConstructor
public class WtaApiSsoClientCatalogAdapter implements SsoClientCatalogPort {

    private final SsoClientCatalog catalog;

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoClientView findByClientId(String clientId) {
        return catalog.findByClientId(clientId);
    }
}
