package org.namewta.system.sso;

import lombok.RequiredArgsConstructor;
import org.namewta.sso.api.SsoClientCatalog;
import org.namewta.sso.api.SsoClientView;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.service.ISysClientService;
import org.springframework.stereotype.Service;

/**
 * 将 sys_client 暴露为 SSO 目录合同。
 */
@Service
@RequiredArgsConstructor
public class SystemSsoClientCatalog implements SsoClientCatalog {

    private final ISysClientService clientService;

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoClientView findByClientId(String clientId) {
        SysClientVo vo = clientService.queryByClientId(clientId);
        if (vo == null) {
            return null;
        }
        SsoClientView view = new SsoClientView();
        view.setId(vo.getId());
        view.setClientId(vo.getClientId());
        view.setClientKey(vo.getClientKey());
        view.setStatus(vo.getStatus());
        view.setSsoEnabled(vo.getSsoEnabled());
        view.setSsoAuthMode(vo.getSsoAuthMode());
        view.setSsoClientKind(vo.getSsoClientKind());
        view.setRedirectUris(vo.getSsoRedirectUriList());
        view.setPkceRequired(vo.getSsoPkceRequired());
        view.setAutoConsent(vo.getSsoAutoConsent());
        view.setScope(vo.getSsoScope());
        view.setDeviceType(vo.getDeviceType());
        view.setTimeout(vo.getTimeout());
        view.setActiveTimeout(vo.getActiveTimeout());
        view.setAccessPath(vo.getAccessPath());
        view.setIpWhitelist(vo.getIpWhitelist());
        view.setUserTypeId(vo.getUserTypeId());
        return view;
    }
}
