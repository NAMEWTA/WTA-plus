package org.namewta.system.sso;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.sso.api.SsoClientView;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.service.ISysClientService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("local")
@Tag("dev")
class ClientAccessPathsTest {

    @Test
    void rawHomeGlobDoesNotAllowIdentityApis() {
        assertFalse(ClientAccessPaths.allows("/home/**", "/system/user/getInfo"));
        assertFalse(ClientAccessPaths.allows("/home/**", "/system/menu/getRouters"));
        assertFalse(ClientAccessPaths.allows("/home/**", "/profile/person/application"));
    }

    @Test
    void homeResolveUnionsIdentityApisHomeWebCallsAndKeepsAdminIsolation() {
        String resolved = ClientAccessPaths.resolve("home", "/home/**");
        assertTrue(ClientAccessPaths.allows(resolved, "/system/user/getInfo"));
        assertTrue(ClientAccessPaths.allows(resolved, "/system/menu/getRouters"));
        assertTrue(ClientAccessPaths.allows(resolved, "/auth/logout"));
        assertTrue(ClientAccessPaths.allows(resolved, "/profile/person/application"));
        assertFalse(ClientAccessPaths.allows(resolved, "/system/client/list"));
        assertFalse(ClientAccessPaths.allows(resolved, "/system/user/list"));
    }

    @Test
    void catalogExposesExpandedHomeAccessPathForSsoTokenExtras() {
        ISysClientService clients = mock(ISysClientService.class);
        SysClientVo vo = new SysClientVo();
        vo.setId(1762000000000000002L);
        vo.setClientId("428a8310cd442757ae699df5d894f051");
        vo.setClientKey("home");
        vo.setAccessPath("/home/**");
        when(clients.queryByClientId("428a8310cd442757ae699df5d894f051")).thenReturn(vo);
        SsoClientView view = new SystemSsoClientCatalog(clients).findByClientId("428a8310cd442757ae699df5d894f051");
        assertTrue(ClientAccessPaths.allows(view.getAccessPath(), "/system/user/getInfo"));
        assertTrue(ClientAccessPaths.allows(view.getAccessPath(), "/system/menu/getRouters"));
        assertFalse(ClientAccessPaths.allows(view.getAccessPath(), "/system/client/list"));
    }
}
