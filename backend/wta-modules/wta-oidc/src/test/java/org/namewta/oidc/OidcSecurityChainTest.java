package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.namewta.oidc.adapter.api.OidcClaims;
import org.namewta.oidc.adapter.api.OidcLoginFilter;
import org.namewta.oidc.adapter.api.OidcOpaqueIntrospector;
import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.config.OidcSecurityConfiguration;
import org.namewta.oidc.service.OidcKeyService;
import org.namewta.oidc.usecase.OidcAuthorizationUseCase;
import org.namewta.oidc.usecase.OidcInteractionUseCase;
import org.namewta.oidc.usecase.OidcProtocolUseCase;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.web.OAuth2AuthorizationEndpointFilter;
import org.springframework.security.web.*;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Tag("dev")
class OidcSecurityChainTest {
    private final WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withUserConfiguration(Fixture.class)
                    .withPropertyValues("namewta.oidc.issuer=https://sso.example");

    @Test
    void narrowChainLeavesSaTokenAndFirstPartySsoAlone() {
        runner.run(
                c -> {
                    assertThat(c).hasNotFailed();
                    var chain = c.getBean(SecurityFilterChain.class);
                    assertThat(chain.matches(new MockHttpServletRequest("GET", "/sso/session")))
                            .isFalse();
                    assertThat(
                                    chain.matches(
                                            new MockHttpServletRequest("GET", "/system/user/list")))
                            .isFalse();
                    assertThat(
                                    chain.matches(
                                            new MockHttpServletRequest(
                                                    "GET", "/oidc/admin/applications")))
                            .isFalse();
                    assertThat(chain.matches(new MockHttpServletRequest("GET", "/oidc/authorize")))
                            .isTrue();
                    var types = chain.getFilters().stream().map(Object::getClass).toList();
                    assertThat(types.indexOf(OidcLoginFilter.class))
                            .isGreaterThan(types.indexOf(SecurityContextHolderFilter.class))
                            .isLessThan(types.indexOf(OAuth2AuthorizationEndpointFilter.class));
                });
    }

    @Test
    void missingKeyStopsTokenButDiscoveryDoesNotAdvertiseRefresh() {
        runner.run(
                c -> {
                    var filter =
                            c.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class);
                    var token = new MockHttpServletResponse();
                    filter.doFilter(
                            new MockHttpServletRequest("POST", "/oidc/token"),
                            token,
                            (a, b) -> fail("fell through"));
                    assertThat(token.getStatus()).isEqualTo(503);
                    var discovery = new MockHttpServletResponse();
                    filter.doFilter(
                            new MockHttpServletRequest("GET", "/.well-known/openid-configuration"),
                            discovery,
                            (a, b) -> fail("fell through"));
                    assertThat(discovery.getStatus()).isEqualTo(200);
                    assertThat(discovery.getContentAsString())
                            .contains("https://sso.example", "wta_person", "authorization_code")
                            .doesNotContain("refresh_token", "registration_endpoint");
                });
    }

    @Test
    void wrongHttpMethodUsesStandardError() {
        runner.run(
                c -> {
                    var response = new MockHttpServletResponse();
                    c.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)
                            .doFilter(
                                    new MockHttpServletRequest("GET", "/oidc/token"),
                                    response,
                                    (a, b) -> fail("fell through"));
                    assertThat(response.getStatus()).isEqualTo(405);
                    assertThat(response.getHeader("Allow")).isEqualTo("POST");
                    assertThat(response.getContentAsString())
                            .isEqualTo("{\"error\":\"invalid_request\"}");
                });
    }

    @Test
    void authenticatedSsoRequestReachesOfficialAuthorizationCodeProvider() {
        authorizeThroughOfficialProvider(false);
    }

    @Test
    void legacySsoCookieWithoutAuthenticationTimeForcesLoginThenResumesNewSession() {
        authorizeThroughOfficialProvider(true);
    }

    private void authorizeThroughOfficialProvider(boolean legacySession) {
        runner.run(
                c -> {
                    when(c.getBean(OidcKeyService.class).ready()).thenReturn(true);
                    var protocol = c.getBean(OidcProtocolUseCase.class);
                    var app = new org.namewta.oidc.domain.OidcApplication();
                    app.setEnabled(true);
                    when(protocol.app("rp")).thenReturn(app);
                    when(protocol.view(app))
                            .thenReturn(
                                    new org.namewta.oidc.domain.vo.OidcApplicationVo(
                                            "1",
                                            "Example",
                                            "rp",
                                            java.util.List.of("https://rp.example/callback"),
                                            java.util.List.of(),
                                            java.util.List.of("nickname"),
                                            java.util.List.of("openid", "profile"),
                                            "client_secret_basic",
                                            true,
                                            true,
                                            0,
                                            null));
                    var now = java.time.Instant.now();
                    when(protocol.session("sid"))
                            .thenReturn(
                                    new org.namewta.sso.api.SsoSessionSnapshot(
                                            1L, "alice", now, now.plusSeconds(3600)));
                    when(protocol.principal(eq("sid"), anySet()))
                            .thenReturn(
                                    new org.namewta.oidc.domain.OidcPrincipal(
                                            "sub",
                                            1L,
                                            "sid",
                                            now.getEpochSecond(),
                                            now.plusSeconds(3600).getEpochSecond(),
                                            java.util.Set.of("nickname")));
                    var client =
                            org.springframework.security.oauth2.server.authorization.client
                                    .RegisteredClient.withId("rp")
                                    .clientId("rp")
                                    .clientAuthenticationMethod(
                                            org.springframework.security.oauth2.core
                                                    .ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                                    .authorizationGrantType(
                                            org.springframework.security.oauth2.core
                                                    .AuthorizationGrantType.AUTHORIZATION_CODE)
                                    .redirectUri("https://rp.example/callback")
                                    .scope("openid")
                                    .scope("profile")
                                    .clientSettings(
                                            org.springframework.security.oauth2.server.authorization
                                                    .settings.ClientSettings.builder()
                                                    .requireProofKey(true)
                                                    .requireAuthorizationConsent(false)
                                                    .build())
                                    .build();
                    when(c.getBean(RegisteredClientRepository.class).findByClientId("rp"))
                            .thenReturn(client);
                    var request = new MockHttpServletRequest("GET", "/oidc/authorize");
                    request.setCookies(new jakarta.servlet.http.Cookie("Sso-Token", "sid"));
                    request.setParameter("client_id", "rp");
                    request.setParameter("redirect_uri", "https://rp.example/callback");
                    request.setParameter("response_type", "code");
                    request.setParameter("scope", "openid profile");
                    request.setParameter("state", "state-a");
                    request.setParameter("nonce", "nonce-a");
                    request.setParameter("code_challenge", "a".repeat(43));
                    request.setParameter("code_challenge_method", "S256");
                    var original = new java.util.LinkedHashMap<String, String>();
                    request.getParameterMap()
                            .forEach((key, values) -> original.put(key, values[0]));
                    String transaction = "t".repeat(43);
                    String browser = "b".repeat(43);
                    when(c.getBean(OidcInteractionUseCase.class).require(transaction, browser))
                            .thenReturn(
                                    new org.namewta.oidc.service.OidcInteractionService.Interaction(
                                            original,
                                            org.namewta.oidc.support.OidcSecrets.hash(browser),
                                            legacySession
                                                    ? org.namewta.oidc.support.OidcSecrets.hash(
                                                            "legacy-sid")
                                                    : "",
                                            legacySession,
                                            now.getEpochSecond(),
                                            "csrf"));
                    if (legacySession) {
                        c.getBean(OidcProperties.class).setSsoWebUrl("https://sso.example");
                        var interactions = c.getBean(OidcInteractionUseCase.class);
                        when(interactions.create(
                                        eq(original), eq(browser), eq("legacy-sid"), eq(true)))
                                .thenReturn(transaction);
                        request.setCookies(
                                new jakarta.servlet.http.Cookie("Sso-Token", "legacy-sid"),
                                new jakarta.servlet.http.Cookie("Oidc-Browser", browser));
                        var login = new MockHttpServletResponse();
                        c.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)
                                .doFilter(
                                        request,
                                        login,
                                        (a, b) -> fail("legacy session reached provider"));
                        assertThat(login.getStatus()).isEqualTo(303);
                        assertThat(login.getHeader("Location"))
                                .isEqualTo("https://sso.example/login?oidc_request=" + transaction);
                        verify(interactions).create(original, browser, "legacy-sid", true);
                        verify(c.getBean(OAuth2AuthorizationService.class), never()).save(any());
                    }
                    request.removeAllParameters();
                    request.setQueryString("oidc_request=" + transaction);
                    request.setParameter("oidc_request", transaction);
                    request.setCookies(
                            new jakarta.servlet.http.Cookie("Sso-Token", "sid"),
                            new jakarta.servlet.http.Cookie("Oidc-Browser", browser));
                    var response = new MockHttpServletResponse();
                    c.getBean("springSecurityFilterChain", jakarta.servlet.Filter.class)
                            .doFilter(request, response, (a, b) -> fail("fell through"));
                    assertThat(response.getStatus()).isEqualTo(302);
                    assertThat(response.getRedirectedUrl())
                            .startsWith("https://rp.example/callback?")
                            .contains("code=")
                            .contains("state=state-a")
                            .doesNotContain("error=");
                    verify(c.getBean(OAuth2AuthorizationService.class))
                            .save(any(OAuth2Authorization.class));
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @Import(OidcSecurityConfiguration.class)
    static class Fixture {
        @Bean
        org.namewta.oidc.service.OidcConfigurationService configuration() {
            var service = mock(org.namewta.oidc.service.OidcConfigurationService.class);
            when(service.current())
                    .thenReturn(
                            new org.namewta.oidc.domain.OidcServiceSettings(
                                    true,
                                    "https://sso.example",
                                    "https://sso.example",
                                    false,
                                    300,
                                    600,
                                    300,
                                    org.namewta.sso.api.SsoRuntimeSettings.defaults()));
            return service;
        }

        @Bean
        org.namewta.sso.api.SsoRuntimeConfiguration centralConfiguration() {
            var service = mock(org.namewta.sso.api.SsoRuntimeConfiguration.class);
            when(service.current()).thenReturn(org.namewta.sso.api.SsoRuntimeSettings.defaults());
            return service;
        }

        @Bean
        OidcKeyService keys() {
            return mock(OidcKeyService.class);
        }

        @Bean
        OidcProtocolUseCase protocol() {
            return mock(OidcProtocolUseCase.class);
        }

        @Bean
        OidcInteractionUseCase interactions() {
            return mock(OidcInteractionUseCase.class);
        }

        @Bean
        OidcAuthorizationUseCase authorizations() {
            return mock(OidcAuthorizationUseCase.class);
        }

        @Bean
        OidcOpaqueIntrospector introspector() {
            return mock(OidcOpaqueIntrospector.class);
        }

        @Bean
        OidcClaims claims() {
            return mock(OidcClaims.class);
        }

        @Bean
        RegisteredClientRepository clients() {
            return mock(RegisteredClientRepository.class);
        }

        @Bean
        OAuth2AuthorizationService storage() {
            return mock(OAuth2AuthorizationService.class);
        }
    }
}
