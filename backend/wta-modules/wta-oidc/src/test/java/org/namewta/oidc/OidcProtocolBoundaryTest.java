package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.namewta.oidc.adapter.api.*;
import org.namewta.oidc.adapter.gateway.WtaOidcIdentityAdapter;
import org.namewta.oidc.config.*;
import org.namewta.oidc.usecase.OidcAuthorizationUseCase;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.system.api.AccountIdentityService;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag("dev")
class OidcProtocolBoundaryTest {
    @Test
    void boundedChunkedFormPreservesRawProtocolValuesAndDuplicateDetection() throws Exception {
        var request =
                new MockHttpServletRequest("POST", "/oidc/token") {
                    @Override
                    public long getContentLengthLong() {
                        return -1;
                    }
                };
        request.setContentType("application/x-www-form-urlencoded");
        request.setQueryString("scope=openid");
        request.setContent(
                "client_secret=a%2Bb%3C%26%3D&scope=profile".getBytes(StandardCharsets.UTF_8));
        new OidcFormBodyFilter()
                .doFilter(
                        request,
                        new MockHttpServletResponse(),
                        (wrapped, response) -> {
                            assertThat(wrapped.getParameter("client_secret")).isEqualTo("a+b<&=");
                            assertThat(wrapped.getParameterValues("scope"))
                                    .containsExactly("openid", "profile");
                        });
        request.setContent(new byte[32769]);
        var response = new MockHttpServletResponse();
        new OidcFormBodyFilter()
                .doFilter(request, response, (a, b) -> fail("oversized body admitted"));
        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void anonymousIntrospectionAndRevocationProduceInvalidClient() {
        var provider =
                new OidcOwnedTokenProvider(
                        mock(AuthenticationProvider.class), mock(OidcAuthorizationUseCase.class));
        var anonymous =
                new AnonymousAuthenticationToken(
                        "key",
                        "anonymous",
                        List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority("anonymous")));
        for (var request :
                List.of(
                        new OAuth2TokenIntrospectionAuthenticationToken(
                                "token", anonymous, null, null),
                        new OAuth2TokenRevocationAuthenticationToken("token", anonymous, null))) {
            assertThatThrownBy(() -> provider.authenticate(request))
                    .isInstanceOfSatisfying(
                            OAuth2AuthenticationException.class,
                            failure ->
                                    assertThat(failure.getError().getErrorCode())
                                            .isEqualTo("invalid_client"));
        }
    }

    @Test
    void insecureTransportRequiresOnlyExplicitDevelopmentProfiles() throws Exception {
        var properties = new OidcProperties();
        properties.setAllowHttp(true);
        var configuration = new OidcSecurityConfiguration();
        for (String[] profiles : new String[][] {{}, {"prod"}, {"dev", "prod"}, {"test"}}) {
            var environment = new MockEnvironment();
            environment.setActiveProfiles(profiles);
            assertThatThrownBy(
                            () ->
                                    configuration
                                            .oidcTransportValidation(properties, environment)
                                            .afterPropertiesSet())
                    .isInstanceOf(IllegalArgumentException.class);
        }
        var development = new MockEnvironment();
        development.setActiveProfiles("dev", "local");
        configuration.oidcTransportValidation(properties, development).afterPropertiesSet();
    }

    @Test
    void disabledFirstPartySsoDoesNotRequireSessionBean() {
        var beans = new DefaultListableBeanFactory();
        var adapter =
                new WtaOidcIdentityAdapter(
                        beans.getBeanProvider(org.namewta.sso.api.SsoSessionAccess.class),
                        mock(AccountIdentityService.class),
                        mock(ProfileDisclosureService.class),
                        beans.getBeanProvider(org.namewta.system.api.OssService.class));
        assertThat(adapter.session("absent")).isNull();
        adapter.logout("absent");
    }
}
