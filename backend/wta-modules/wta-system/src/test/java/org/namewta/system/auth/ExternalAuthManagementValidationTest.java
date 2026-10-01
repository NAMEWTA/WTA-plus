package org.namewta.system.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.system.domain.*;
import org.namewta.system.domain.bo.*;
import org.namewta.system.mapper.*;
import org.namewta.system.service.impl.SysExternalAuthConfigServiceImpl;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 输入与引用不变量在任何持久化或密钥写入之前失败关闭。 */
@Tag("dev")
class ExternalAuthManagementValidationTest {
    private final SysAuthProviderMapper providers = mock(SysAuthProviderMapper.class);
    private final SysAuthRegistrationMapper registrations = mock(SysAuthRegistrationMapper.class);
    private final SysClientMapper clients = mock(SysClientMapper.class);
    private final SysExternalAuthConfigServiceImpl service = new SysExternalAuthConfigServiceImpl(providers, registrations,
        clients, mock(SocialSecretCipher.class), mock(ApplicationEventPublisher.class), mock(ExternalAuthConfigurationCache.class), mock(org.namewta.common.social.oidc.OidcProtocolClient.class));

    @ParameterizedTest
    @ValueSource(strings = {"", "*", "../other", "key with spaces"})
    void invalidProviderKeysCannotWrite(String key) {
        var bo = provider(); bo.setProviderKey(key);
        assertThatThrownBy(() -> service.saveProvider(bo, true)).isInstanceOf(ServiceException.class);
        verifyNoInteractions(providers);
    }

    @ParameterizedTest
    @ValueSource(strings = {"file:///etc/passwd", "https://*.example", "https://a.example/#fragment", "https://user:pass@a.example"})
    void callbackMustBeAnExactHttpAddress(String uri) {
        var bo = registration(); bo.setRedirectUri(uri);
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).isInstanceOf(ServiceException.class);
        verifyNoInteractions(providers, registrations, clients);
    }

    @ParameterizedTest
    @ValueSource(strings = {"clientSecret", "privateKey", "password", "access_token", "refresh-token"})
    void optionsCannotExposeSecrets(String key) {
        var bo = provider(); bo.setOptions(Map.of(key, "private"));
        assertThatThrownBy(() -> service.saveProvider(bo, true)).isInstanceOf(ServiceException.class);
        verifyNoInteractions(providers);
    }

    @Test
    void issuerCannotContainQueryAndUnknownProtocolsAreRejected() {
        var bo = provider(); bo.setIssuer("https://issuer.example?tenant=one");
        assertThatThrownBy(() -> service.saveProvider(bo, true)).isInstanceOf(ServiceException.class);
        bo.setIssuer("https://issuer.example"); bo.setProtocol("UNKNOWN");
        assertThatThrownBy(() -> service.saveProvider(bo, true)).isInstanceOf(ServiceException.class);
    }

    @Test
    void invalidScopeAndFirstLoginPolicyAreRejected() {
        var bo = registration(); bo.setScopes(List.of("openid profile"));
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).isInstanceOf(ServiceException.class);
        bo.setScopes(List.of("openid")); bo.setFirstLoginPolicy("TRUST_EMAIL");
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).isInstanceOf(ServiceException.class);
    }

    @Test
    void unknownInactiveOrNonSocialClientCannotRegister() {
        var provider = new SysAuthProvider(); provider.setId(1L); when(providers.selectForUpdate(1L)).thenReturn(provider);
        var bo = registration();
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).isInstanceOf(ServiceException.class);
        var client = new SysClient(); client.setStatus("1"); client.setGrantType("social");
        when(clients.selectOne(any())).thenReturn(client);
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).isInstanceOf(ServiceException.class);
        client.setStatus("0"); client.setGrantType("password");
        assertThatThrownBy(() -> service.saveRegistration(bo, true)).isInstanceOf(ServiceException.class);
        verifyNoInteractions(registrations);
    }

    @Test
    void staleProviderVersionAndReferencedIdentityMutationAreRejected() {
        var previous = new SysAuthProvider(); previous.setId(1L); previous.setVersion(5L); previous.setProviderKey("corporate");
        previous.setProtocol("OIDC"); previous.setIssuer("https://issuer.example");
        when(providers.selectForUpdate(1L)).thenReturn(previous);
        var bo = provider(); bo.setId(1L); bo.setVersion(4L);
        assertThatThrownBy(() -> service.saveProvider(bo, false)).isInstanceOf(ServiceException.class);
        bo.setVersion(5L); bo.setIssuer("https://other.example"); when(registrations.countAllForProvider(1L)).thenReturn(1L);
        assertThatThrownBy(() -> service.saveProvider(bo, false)).isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可修改");
        verify(providers, never()).updateById(any(SysAuthProvider.class));
    }

    private SysAuthProviderBo provider() {
        var bo = new SysAuthProviderBo(); bo.setProviderKey("corporate"); bo.setName("企业身份");
        bo.setProtocol("OIDC"); bo.setIssuer("https://issuer.example"); bo.setEnabled(true); return bo;
    }

    private SysAuthRegistrationBo registration() {
        var bo = new SysAuthRegistrationBo(); bo.setProviderId(1L); bo.setBusinessClientId("home"); bo.setExternalClientId("rp-home");
        bo.setRedirectUri("https://home.example/callback"); bo.setFirstLoginPolicy("BIND_ONLY"); bo.setEnabled(true); return bo;
    }
}
