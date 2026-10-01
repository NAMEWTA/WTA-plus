package org.namewta.web.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.system.api.ExternalAuthConfigurationService;
import org.namewta.system.api.model.ExternalAuthEntry;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysClientService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("local")
@Tag("dev")
class AuthClientContextSsoUnitTest {
    @Test
    void exposesOnlyDatabaseConfiguredEntriesForTheRequestedClient() {
        var clients = mock(ISysClientService.class);
        var configurations = mock(ExternalAuthConfigurationService.class);
        var client = activeClient("password,social");
        when(clients.queryByClientId("home")).thenReturn(client);
        when(configurations.listEnabled("home")).thenReturn(List.of(new ExternalAuthEntry("company", "统一登录", "tabler:login", "OIDC")));
        var context = controller(clients, configurations).clientContext("home", null).getData();
        assertEquals("both", context.getAuthMode());
        assertEquals("company", context.getProviders().getFirst().providerKey());
        assertFalse(context.getSsoEnabled());
        assertNull(context.getSsoAuthorizeUrl());
        verify(configurations).listEnabled("home");
    }

    @Test
    void preservesLocalBootstrapWhenNoExternalProviderIsConfigured() {
        var clients = mock(ISysClientService.class);
        var configurations = mock(ExternalAuthConfigurationService.class);
        var client = activeClient("password,social");
        client.setSsoAuthMode("sso");
        when(clients.queryByClientId("home")).thenReturn(client);
        when(configurations.listEnabled("home")).thenReturn(List.of());
        var context = controller(clients, configurations).clientContext(null, "home").getData();
        assertEquals("local", context.getAuthMode());
        assertTrue(context.getClientEnabled());
        assertTrue(context.getProviders().isEmpty());
    }

    @Test
    void disabledOrNonSocialClientsCannotDiscoverAnotherClientsProviders() {
        var clients = mock(ISysClientService.class);
        var configurations = mock(ExternalAuthConfigurationService.class);
        var client = activeClient("password,notsocial");
        when(clients.queryByClientId("home")).thenReturn(client);
        assertTrue(controller(clients, configurations).clientContext("home", null).getData().getProviders().isEmpty());
        client.setGrantType("social");
        client.setStatus(SystemConstants.DISABLE);
        assertTrue(controller(clients, configurations).clientContext("home", null).getData().getProviders().isEmpty());
        verifyNoInteractions(configurations);
    }

    private static AuthController controller(ISysClientService clients, ExternalAuthConfigurationService configurations) {
        return new AuthController(null, null, null, clients, null, mock(PasswordPolicyService.class), configurations, null);
    }

    private static SysClientVo activeClient(String grant) {
        var client = new SysClientVo();
        client.setStatus(SystemConstants.NORMAL);
        client.setGrantType(grant);
        return client;
    }
}
