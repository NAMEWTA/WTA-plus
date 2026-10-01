package org.namewta.system.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.common.social.oidc.OidcProtocolClient;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.domain.*;
import org.namewta.system.domain.bo.SysAuthRegistrationBo;
import org.namewta.system.mapper.*;
import org.namewta.system.service.impl.SysExternalAuthConfigServiceImpl;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 管理专用跨 App 目录与协议配置边界，不替代普通业务 Client/RBAC。 */
@Tag("dev")
class ExternalAuthManagementAccessTest {
    private final SysAuthProviderMapper providers = mock(SysAuthProviderMapper.class);
    private final SysAuthRegistrationMapper registrations = mock(SysAuthRegistrationMapper.class);
    private final SysClientMapper clients = mock(SysClientMapper.class);
    private final SysExternalAuthConfigServiceImpl service = new SysExternalAuthConfigServiceImpl(providers, registrations,
        clients, mock(SocialSecretCipher.class), mock(ApplicationEventPublisher.class), mock(ExternalAuthConfigurationCache.class),
        mock(OidcProtocolClient.class));

    @org.junit.jupiter.api.BeforeAll
    static void initializeLambdaMetadata() {
        var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "auth-management-unit");
        for (var entity : List.of(SysClient.class, SysAuthProvider.class, SysAuthRegistration.class)) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, entity);
        }
    }

    @Test
    void oidcRequiresOpenidEffectiveSecretAndSupportedAuthenticationMethod() {
        var source = prepareSource();
        var bo = registration(); bo.setScopes(List.of("profile")); bo.setClientSecret("test-only");
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).hasMessageContaining("openid");
        bo.setScopes(List.of("openid", "profile")); bo.setClientSecret("");
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).hasMessageContaining("Client Secret");
        bo.setClientSecret("test-only"); bo.setOptions(Map.of("authenticationMethod", "private_key_jwt"));
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).hasMessageContaining("认证方式");
        bo.setOptions(Map.of()); source.setOptionsJson("{\"authenticationMethod\":\"none\"}");
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).hasMessageContaining("认证方式");
        verify(registrations, never()).insert(any(SysAuthRegistration.class));
    }

    @Test
    void malformedPublicApiBaseCannotGenerateMisleadingCallback() {
        prepareSource();
        var bo = registration(); bo.setScopes(List.of("openid")); bo.setClientSecret("test-only");
        bo.setOptions(Map.of("apiPublicBase", "https://api.example/api?secret=bad"));
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).hasMessageContaining("apiPublicBase");
        verify(registrations, never()).insert(any(SysAuthRegistration.class));
    }

    @Test
    void adminManagementRejectsOtherClientEvenForSuperAdministrator() {
        try (var helper = mockStatic(LoginHelper.class)) {
            var login = new LoginUser(); login.setClientPk(2L); login.setUserId(1L);
            helper.when(LoginHelper::getLoginUser).thenReturn(login);
            var client = new SysClient(); client.setClientKey("home"); client.setStatus("0");
            when(clients.selectById(2L)).thenReturn(client);
            assertThatThrownBy(service::requireAdminClient).hasMessageContaining("Admin");
            client.setClientKey("pc"); service.requireAdminClient();
            client.setStatus("1"); assertThatThrownBy(service::requireAdminClient).hasMessageContaining("Admin");
            login.setClientPk(null); assertThatThrownBy(service::requireAdminClient).hasMessageContaining("Admin");
            helper.when(LoginHelper::getLoginUser).thenReturn(null);
            assertThatThrownBy(service::requireAdminClient).hasMessageContaining("Admin");
        }
    }

    @Test
    void connectionInfoUsesBusinessApiPrefixAndInheritedAuthenticationWithoutSecrets() {
        var source = prepareSource(); source.setName("Corporate"); source.setProviderKey("corporate");
        source.setIssuer("https://issuer.example/realm"); source.setOptionsJson("{\"authenticationMethod\":\"client_secret_post\"}");
        when(providers.selectById(1L)).thenReturn(source);
        var row = new SysAuthRegistration(); row.setId(12L); row.setProviderId(1L); row.setBusinessClientId("home-oauth");
        row.setExternalClientId("remote-home"); row.setRedirectUri("https://home.example/app/social-callback");
        row.setScopesJson("[\"openid\"]"); row.setClientSecretCiphertext("must-not-appear");
        row.setOptionsJson("{\"apiPublicBase\":\"https://api.example/prod-api/\"}");
        when(registrations.selectById(12L)).thenReturn(row);
        var result = service.connectionInfo(12L);
        assertThat(result.authenticationMethod()).isEqualTo("client_secret_post");
        assertThat(result.backchannelLogoutUri()).isEqualTo("https://api.example/prod-api/auth/social/backchannel/12");
        assertThat(result.discoveryUrl()).isEqualTo("https://issuer.example/realm/.well-known/openid-configuration");
        assertThat(result.secretConfigured()).isTrue();
        assertThat(result.toString()).doesNotContain("must-not-appear");
        row.setOptionsJson("{}"); assertThat(service.connectionInfo(12L).backchannelLogoutUri()).isEmpty();
    }

    @Test
    void clientDirectoryCountsEffectiveEntriesAcrossAppsWithoutFetchingCredentials() {
        var active = new SysClient(); active.setClientId("home-oauth"); active.setClientKey("home");
        active.setStatus("0"); active.setGrantType("password, social"); active.setRegisterEnabled(true);
        var disabled = new SysClient(); disabled.setClientId("admin-oauth"); disabled.setClientKey("pc");
        disabled.setStatus("1"); disabled.setGrantType("social");
        when(clients.selectList(any())).thenReturn(List.of(active, disabled));
        var row1 = new SysAuthRegistration(); row1.setBusinessClientId("home-oauth"); row1.setProviderId(1L); row1.setEnabled(true);
        var row2 = new SysAuthRegistration(); row2.setBusinessClientId("home-oauth"); row2.setProviderId(2L); row2.setEnabled(true);
        var row3 = new SysAuthRegistration(); row3.setBusinessClientId("admin-oauth"); row3.setProviderId(1L); row3.setEnabled(true);
        when(registrations.selectList(any())).thenReturn(List.of(row1, row2, row3));
        var source = new SysAuthProvider(); source.setId(1L);
        when(providers.selectList(any())).thenReturn(List.of(source));
        var results = service.clientOptions(null, List.of("home-oauth", "admin-oauth"));
        assertThat(results.get(0).providerCount()).isEqualTo(2);
        assertThat(results.get(0).enabledProviderCount()).isEqualTo(1);
        assertThat(results.get(1).enabledProviderCount()).isZero();
        assertThat(results.get(1).unavailableReason()).isEqualTo("客户端已停用");
        verify(clients).selectList(argThat((com.baomidou.mybatisplus.core.conditions.Wrapper<SysClient> wrapper) ->
            !wrapper.getSqlSelect().contains("secret") && !wrapper.getSqlSelect().contains("whitelist")));
        verify(registrations).selectList(argThat((com.baomidou.mybatisplus.core.conditions.Wrapper<SysAuthRegistration> wrapper) ->
            !wrapper.getSqlSelect().contains("secret")));
    }

    @Test
    void registrationProviderDirectoryProjectsOnlyAuthenticationDefaults() {
        var source = prepareSource(); source.setName("Source"); source.setEnabled(true);
        source.setOptionsJson("{\"authenticationMethod\":\"client_secret_post\",\"serverUrl\":\"not-in-directory\"}");
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysAuthProvider>();
        page.setRecords(List.of(source));
        when(providers.selectPage(any(), any())).thenReturn(page);
        var row = service.providerOptions("Source", 1L).getFirst();
        assertThat(row.options()).containsExactlyEntriesOf(Map.of("authenticationMethod", "client_secret_post"));
        assertThat(row.toString()).doesNotContain("not-in-directory");
        verify(providers, never()).selectById(anyLong());
    }

    private SysAuthProvider prepareSource() {
        var source = new SysAuthProvider(); source.setId(1L); source.setProtocol("OIDC");
        source.setIssuer("https://issuer.example"); source.setOptionsJson("{}");
        when(providers.selectForUpdate(1L)).thenReturn(source);
        var client = new SysClient(); client.setStatus("0"); client.setGrantType("password,social");
        when(clients.selectOne(any())).thenReturn(client);
        return source;
    }

    private SysAuthRegistrationBo registration() {
        var bo = new SysAuthRegistrationBo(); bo.setProviderId(1L); bo.setBusinessClientId("home"); bo.setExternalClientId("rp-home");
        bo.setRedirectUri("https://home.example/callback"); bo.setFirstLoginPolicy("BIND_ONLY"); bo.setEnabled(true); return bo;
    }
}
