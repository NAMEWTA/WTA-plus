package org.namewta.web.controller;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.sso.config.SsoProperties;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysClientService;
import org.namewta.web.domain.vo.AuthClientContextVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("local")
@Tag("dev")
class AuthClientContextSsoUnitTest {

    @Test
    void publishedBasePathIsIncludedInTheActualClientContext() {
        ISysClientService clientService = mock(ISysClientService.class);
        SysClientVo client = new SysClientVo();
        client.setStatus(SystemConstants.NORMAL);
        client.setSsoEnabled(true);
        when(clientService.queryByClientId("home")).thenReturn(client);
        SsoProperties properties = new SsoProperties();
        properties.setWebOrigin("https://sso.example.invalid/");
        properties.setWebBasePath("/sso-app/");
        AuthController controller = new AuthController(null, null, null, null, clientService, null,
            mock(PasswordPolicyService.class), properties);

        assertEquals("https://sso.example.invalid/sso-app/authorize", controller.clientContext("home", null).getData().getSsoAuthorizeUrl());
        for (String invalid : new String[]{"//foreign/", "/../", "/sso?next=/", "sso-app", "/sso-app", "/nested/path/"}) {
            assertThrows(IllegalArgumentException.class, () -> properties.setWebBasePath(invalid));
        }
    }

    @Test
    void exposesSsoContextFieldsWhenEnabled() {
        ISysClientService clientService = mock(ISysClientService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        SysClientVo client = new SysClientVo();
        client.setStatus(SystemConstants.NORMAL);
        client.setSsoEnabled(true);
        client.setSsoAuthMode("both");
        when(clientService.queryByClientId("e5cd7e4891bf95d1d19206ce24a7b32e")).thenReturn(client);
        SsoProperties properties = new SsoProperties();
        properties.setEnabled(true);
        properties.setWebOrigin("http://127.0.0.1:4176");
        AuthController controller = new AuthController(null, null, null, null, clientService, null, policyService, properties);

        AuthClientContextVo vo = controller.clientContext("e5cd7e4891bf95d1d19206ce24a7b32e", null).getData();
        assertTrue(vo.getSsoEnabled());
        assertEquals("http://127.0.0.1:4176/authorize", vo.getSsoAuthorizeUrl());
        assertEquals("both", vo.getAuthMode());
    }

    @Test
    void keepsLocalLoginWhenAuthModeBoth() {
        ISysClientService clientService = mock(ISysClientService.class);
        SysClientVo client = new SysClientVo();
        client.setStatus(SystemConstants.NORMAL);
        client.setSsoEnabled(true);
        client.setSsoAuthMode("sso");
        when(clientService.queryByClientId("home")).thenReturn(client);
        SsoProperties properties = new SsoProperties();
        properties.setEnabled(true);
        properties.setWebOrigin("http://127.0.0.1:4176/");
        AuthController controller = new AuthController(null, null, null, null, clientService, null, mock(PasswordPolicyService.class), properties);
        AuthClientContextVo vo = controller.clientContext("home", null).getData();
        assertEquals("sso", vo.getAuthMode());
        assertTrue(vo.getClientEnabled());
        assertFalse(Boolean.TRUE.equals(vo.getRegisterEnabled()));
    }
}
