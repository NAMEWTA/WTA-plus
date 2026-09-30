package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.namewta.oidc.usecase.*;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

@Tag("dev")
class OidcAuthorizationTransactionTest {
    @Test
    void consumedCodeIsNotReopenedWhenFinalizationFails() {
        var flow = mock(OidcAuthorizationWorkflow.class);
        var consumption = mock(OidcConsumeUseCase.class);
        var value = mock(OAuth2Authorization.class);
        when(value.getRegisteredClientId()).thenReturn("client");
        when(consumption.consume("code", "client")).thenReturn(true);
        doThrow(new IllegalStateException("storage unavailable")).when(flow).finish(value);
        var useCase = new OidcAuthorizationUseCase(flow, consumption);
        assertThatThrownBy(() -> useCase.save(value, "code"))
                .isInstanceOf(IllegalStateException.class);
        var order = inOrder(consumption, flow);
        order.verify(consumption).consume("code", "client");
        order.verify(flow).finish(value);
        verifyNoMoreInteractions(consumption, flow);
    }

    @Test
    void losingConsumerCannotPublishTokens() {
        var flow = mock(OidcAuthorizationWorkflow.class);
        var consumption = mock(OidcConsumeUseCase.class);
        var value = mock(OAuth2Authorization.class);
        when(value.getRegisteredClientId()).thenReturn("client");
        var useCase = new OidcAuthorizationUseCase(flow, consumption);
        assertThatThrownBy(() -> useCase.save(value, "code"))
                .isInstanceOf(
                        org.springframework.security.oauth2.core.OAuth2AuthenticationException
                                .class);
        verify(flow, never()).finish(any());
    }

    @Test
    void inFlightAuthorizationCannotSurviveApplicationPolicyChange() {
        var persistence = mock(org.namewta.oidc.service.OidcAuthorizationPersistenceService.class);
        var apps = mock(org.namewta.oidc.service.OidcApplicationService.class);
        var identity = mock(org.namewta.oidc.service.OidcIdentityService.class);
        var keys = mock(org.namewta.oidc.service.OidcKeyService.class);
        var workflow = new OidcAuthorizationWorkflow(persistence, apps, identity, keys);
        var value = mock(OAuth2Authorization.class);
        when(value.getRegisteredClientId()).thenReturn("client");
        when(value.getId()).thenReturn("authorization");
        var principal =
                new org.namewta.oidc.domain.OidcPrincipal(
                        "sub", 1L, "sid", 1, 2, java.util.Set.of(), 1);
        when(value.getAttribute(java.security.Principal.class.getName()))
                .thenReturn(
                        org.springframework.security.authentication
                                .UsernamePasswordAuthenticationToken.authenticated(
                                principal, null, java.util.List.of()));
        var request =
                org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
                        .authorizationCode()
                        .authorizationUri("https://sso.example/oidc/authorize")
                        .clientId("client")
                        .redirectUri("https://rp.example/old")
                        .scope("openid")
                        .build();
        when(value.getAttribute(
                        org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
                                .class
                                .getName()))
                .thenReturn(request);
        var current = new org.namewta.oidc.domain.OidcApplication();
        current.setEnabled(true);
        current.setVersion(2);
        current.setRedirectUrisJson("new");
        when(apps.lockClient("client")).thenReturn(current);
        when(apps.values("new")).thenReturn(java.util.List.of("https://rp.example/new"));
        assertThatThrownBy(() -> workflow.saveInitial(value))
                .isInstanceOf(
                        org.springframework.security.oauth2.core.OAuth2AuthenticationException
                                .class);
        current.setVersion(1);
        assertThatThrownBy(() -> workflow.saveInitial(value))
                .isInstanceOf(
                        org.springframework.security.oauth2.core.OAuth2AuthenticationException
                                .class);
        verify(persistence, never()).insert(any());
    }
}
