package org.namewta.web.service.social;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.common.social.oidc.OidcProtocolClient;
import org.namewta.system.api.ExternalAuthConfigurationService;
import org.namewta.system.api.model.ExternalAuthRegistration;
import org.namewta.system.api.model.SocialLoginBody;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.service.ClientUserTypeAccessService;
import org.namewta.system.service.ISysClientService;
import org.namewta.web.domain.bo.ExternalRegisterBo;
import org.namewta.web.domain.vo.LoginVo;
import org.namewta.web.service.SysLoginService;

import java.util.List;
import java.util.Map;

@Tag("dev")
class ExternalAuthServiceTest {
    private final ExternalAuthConfigurationService configs =
            mock(ExternalAuthConfigurationService.class);
    private final OidcProtocolClient protocol = mock(OidcProtocolClient.class);
    private final ExternalAuthStateStore transactions = mock(ExternalAuthStateStore.class);
    private final ExternalAuthAccountService accounts = mock(ExternalAuthAccountService.class);
    private final ExternalAuthSessionStore sessions = mock(ExternalAuthSessionStore.class);
    private final ISysClientService clients = mock(ISysClientService.class);
    private final ClientUserTypeAccessService access = mock(ClientUserTypeAccessService.class);
    private final SysLoginService login = mock(SysLoginService.class);
    private final ExternalAuthService service =
            new ExternalAuthService(
                    configs, protocol, transactions, accounts, sessions, clients, access, login);
    private final SysClientVo client = new SysClientVo();
    private final SocialLoginBody request = new SocialLoginBody();
    private final ExternalAuthStateStore.Transaction transaction =
            new ExternalAuthStateStore.Transaction(
                    "sso",
                    "home",
                    1,
                    2,
                    3,
                    "LOGIN",
                    null,
                    "/profile",
                    "browserHash",
                    "nonce",
                    "verifier");
    private ExternalAuthRegistration registration;

    @BeforeEach
    void setup() {
        client.setClientId("home");
        client.setGrantType("password,social");
        client.setStatus("0");
        client.setRegisterEnabled(true);
        request.setSource("sso");
        request.setClientId("home");
        request.setSocialState("state");
        request.setSocialCode("code");
        request.setTransactionKey("browser");
        registration = registration("AUTO_REGISTER");
        when(transactions.consume("state", "browser", "home", "sso", "LOGIN", null))
                .thenReturn(transaction);
        when(configs.require("sso", "home")).thenAnswer(i -> registration);
        when(configs.isCurrent(1, 2, 3)).thenReturn(true);
        when(clients.queryByClientId("home")).thenReturn(client);
        when(protocol.exchange(any(), eq("code"), eq("verifier"), eq("nonce")))
                .thenReturn(identity(null));
        when(accounts.findUser(any(), anyString())).thenReturn(null);
        when(accounts.withBoundIdentity(anyLong(), any(), anyString(), any()))
                .thenAnswer(
                        call -> call.<java.util.function.Supplier<LoginVo>>getArgument(3).get());
    }

    @Test
    void missingPhoneReturnsOnlyAPendingTicketNotABusinessToken() {
        when(transactions.pending(eq(transaction), any())).thenReturn("pending-ticket");
        var result = service.login(request, client);
        assertThat(result.getNextAction()).isEqualTo("COMPLETE_PROFILE");
        assertThat(result.getAccessToken()).isNull();
        assertThat(result.getRequiredFields()).containsExactly("phoneNumber");
        assertThat(result.getRegistrationTicket()).isEqualTo("pending-ticket");
        verifyNoInteractions(sessions, access);
    }

    @Test
    void adminBindingPolicyAndClosedRegistrationNeverCreateUsers() {
        registration = registration("BIND_ONLY");
        assertThat(service.login(request, client).getNextAction()).isEqualTo("BIND_REQUIRED");
        registration = registration("AUTO_REGISTER");
        client.setRegisterEnabled(false);
        assertThat(service.login(request, client).getNextAction()).isEqualTo("BIND_REQUIRED");
        verify(accounts, never()).register(any(), anyString(), anyString(), any(), anyString());
        verifyNoInteractions(sessions);
    }

    @Test
    void phoneConflictRequestsExplicitBindingAndDoesNotGrantAClient() {
        when(protocol.exchange(any(), anyString(), anyString(), anyString()))
                .thenReturn(identity("13800000001"));
        when(accounts.register(eq(client), eq("sso"), eq("OIDC"), any(), eq("13800000001")))
                .thenReturn(null);
        assertThat(service.login(request, client).getNextAction()).isEqualTo("BIND_REQUIRED");
        verifyNoInteractions(access, sessions);
    }

    @Test
    void changedConfigRejectsCallbackBeforeExchangingCode() {
        when(configs.isCurrent(1, 2, 3)).thenReturn(false);
        assertThatThrownBy(() -> service.login(request, client))
                .isInstanceOf(ServiceException.class);
        verifyNoInteractions(protocol, accounts, sessions);
    }

    @Test
    void validExternalIdentityStillNeedsBusinessClientAccess() {
        when(accounts.findUser(any(), eq("OIDC"))).thenReturn(77L);
        when(accounts.requireUser(77L)).thenReturn(new SysUserVo());
        when(access.requireLoginAccess(77L, client)).thenThrow(new ServiceException("无权登录当前应用"));
        assertThatThrownBy(() -> service.login(request, client))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("无权");
        verifyNoInteractions(sessions);
    }

    @Test
    void invalidPhoneDoesNotConsumeThePendingTicket() {
        assertThatThrownBy(
                        () ->
                                service.register(
                                        new ExternalRegisterBo(
                                                "home", "ticket", "browser", "invalid")))
                .isInstanceOf(ServiceException.class);
        verifyNoInteractions(transactions, accounts, sessions);
    }

    @Test
    void oidcCannotUseTheLegacyUnboundCallback() {
        request.setTransactionKey(null);
        assertThatThrownBy(() -> service.login(request, client))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("事务");
        verifyNoInteractions(protocol, accounts, sessions);
    }

    @Test
    void justAuthCannotDowngradeLoginOrBindingByDroppingTheBrowserTransactionKey() {
        request.setSource("github");
        for (String missing : new String[] {null, "", " "}) {
            request.setTransactionKey(missing);
            assertThatThrownBy(() -> service.login(request, client))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("浏览器授权事务");
            assertThatThrownBy(() -> service.bind(request))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("浏览器授权事务");
        }
        verifyNoInteractions(configs, protocol, transactions, accounts, sessions, clients);
    }

    @Test
    void legacyAuthorizationRequiresCurrentFrontendWithoutIssuingAnotherState() {
        assertThatThrownBy(() -> service.legacyAuthorize("github", "home"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("旧第三方登录入口已停用", "当前应用登录页面");
        verifyNoInteractions(configs, protocol, transactions, accounts, sessions, clients);
    }

    @Test
    void loginCannotMintAfterTheIdentityWasUnboundFollowingTheInitialLookup() {
        when(accounts.findUser(any(), eq("OIDC"))).thenReturn(77L);
        when(accounts.withBoundIdentity(eq(77L), any(), eq("OIDC"), any()))
                .thenThrow(new ServiceException("第三方身份绑定已变更"));
        assertThatThrownBy(() -> service.login(request, client))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("绑定已变更");
        verify(accounts, never()).requireUser(anyLong());
        verifyNoInteractions(sessions, access, login);
    }

    @Test
    void returnPathsCannotEscapeTheApp() {
        for (String path :
                List.of(
                        "https://foreign.example",
                        "//foreign.example",
                        "/\\foreign",
                        "/%2fexample",
                        "/path\n"))
            assertThatThrownBy(() -> ExternalAuthService.safeReturnPath(path))
                    .isInstanceOf(ServiceException.class);
        assertThat(ExternalAuthService.safeReturnPath("/profile?tab=person"))
                .isEqualTo("/profile?tab=person");
    }

    private ExternalAuthRegistration registration(String policy) {
        return new ExternalAuthRegistration(
                1,
                2,
                3,
                "sso",
                "OIDC",
                "https://sso.example",
                "SSO",
                "tabler:login",
                "home",
                "home-rp",
                "secret",
                "https://home.example/social-callback",
                "https://home.example/logout/callback",
                List.of("openid", "profile", "phone"),
                policy,
                Map.of());
    }

    private OidcIdentity identity(String phone) {
        return new OidcIdentity(
                "https://sso.example",
                "subject-a",
                "sid-a",
                "测试",
                phone,
                null,
                "id-token",
                "https://sso.example/logout",
                1);
    }
}
