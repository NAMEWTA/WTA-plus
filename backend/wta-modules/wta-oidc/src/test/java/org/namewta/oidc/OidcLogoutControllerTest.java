package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.controller.anonymous.OidcLogoutController;
import org.namewta.oidc.domain.*;
import org.namewta.oidc.service.OidcInteractionService.Interaction;
import org.namewta.oidc.support.OidcSecrets;
import org.namewta.oidc.usecase.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.security.Principal;
import java.util.*;

@Tag("dev")
class OidcLogoutControllerTest {
    private final OidcAuthorizationUseCase authorizations = mock(OidcAuthorizationUseCase.class);
    private final OidcProtocolUseCase protocol = mock(OidcProtocolUseCase.class);
    private final OidcInteractionUseCase interactions = mock(OidcInteractionUseCase.class);
    private final OidcProperties properties = new OidcProperties();
    private final OidcLogoutController controller =
            new OidcLogoutController(authorizations, protocol, interactions, properties);
    private final String transaction = "t".repeat(43), browser = "b".repeat(43);
    private final OidcApplication app = new OidcApplication();

    @BeforeEach
    void prepare() {
        properties.setSsoWebUrl("https://sso.example");
        when(protocol.app("rp")).thenReturn(app);
        var value = mock(OAuth2Authorization.class);
        when(authorizations.logoutHint("hint")).thenReturn(value);
        when(value.getRegisteredClientId()).thenReturn("rp");
        when(value.getAttribute(Principal.class.getName()))
                .thenReturn(
                        UsernamePasswordAuthenticationToken.authenticated(
                                new OidcPrincipal("sub", 1L, "sid", 1, 2, Set.of()),
                                null,
                                List.of()));
        when(interactions.create(anyMap(), eq(browser), eq("sid"), eq(false)))
                .thenReturn(transaction);
        when(interactions.require(transaction, browser))
                .thenReturn(
                        new Interaction(
                                Map.of("kind", "logout", "client_id", "rp"),
                                OidcSecrets.hash(browser),
                                OidcSecrets.hash("sid"),
                                false,
                                1,
                                "csrf"));
    }

    private MockHttpServletRequest request(String method) {
        var request = new MockHttpServletRequest(method, "/oidc/logout");
        request.setCookies(
                new jakarta.servlet.http.Cookie("Sso-Token", "sid"),
                new jakarta.servlet.http.Cookie("Oidc-Browser", browser));
        request.setParameter("id_token_hint", "hint");
        request.setParameter("oidc_request", transaction);
        return request;
    }

    @Test
    void confirmationAllowsOnlyRegisteredRedirectOriginAndSameOriginSubmission() throws Exception {
        var request = request("GET");
        request.setParameter("post_logout_redirect_uri", "https://rp.example:444/end?state=a");
        app.setPostLogoutRedirectUrisJson("callbacks");
        when(protocol.values("callbacks"))
                .thenReturn(List.of("https://rp.example:444/end?state=a"));
        var response = new MockHttpServletResponse();
        controller.begin(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("same-origin");
        assertThat(response.getHeader("Content-Security-Policy"))
                .contains("form-action 'self' https://rp.example:444;")
                .doesNotContain("state=a");
        assertThat(response.getContentAsString())
                .contains("https://sso.example/oidc-theme.css", "name=csrf", "oidc-card");
    }

    @Test
    void invalidCsrfCannotRevokeCentralSession() throws Exception {
        var request = request("POST");
        request.setParameter("csrf", "wrong");
        var response = new MockHttpServletResponse();
        controller.finish(request, response);
        assertThat(response.getStatus()).isEqualTo(400);
        verify(authorizations, never()).revokeSession(any());
        verify(protocol, never()).logout(any());
        verify(interactions, never()).consume(any(), any());
    }

    @Test
    void completionWithoutRedirectRetainsSharedUiAndExpiresCookie() throws Exception {
        var request = request("POST");
        request.setParameter("csrf", "csrf");
        var response = new MockHttpServletResponse();
        controller.finish(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("oidc-theme.css", "oidc-card");
        assertThat(response.getHeader("Set-Cookie"))
                .contains("Sso-Token=", "Max-Age=0", "Secure", "HttpOnly");
        verify(authorizations).revokeSession("sid");
        verify(protocol).logout("sid");
    }
}
