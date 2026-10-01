package org.namewta.system.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.system.domain.SysAuthProvider;
import org.namewta.system.domain.SysAuthRegistration;
import org.namewta.system.domain.read.ExternalAuthRuntimeStamp;
import org.namewta.system.mapper.SysAuthProviderMapper;
import org.namewta.system.mapper.SysAuthRegistrationMapper;
import org.namewta.system.service.impl.ExternalAuthConfigurationServiceImpl;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 运行时配置不能依赖缓存中的旧启用状态或旧版本。 */
@Tag("dev")
class ExternalAuthConfigurationServiceTest {
    private final SysAuthProviderMapper providers = mock(SysAuthProviderMapper.class);
    private final SysAuthRegistrationMapper registrations = mock(SysAuthRegistrationMapper.class);
    private final ExternalAuthConfigurationCache cache = mock(ExternalAuthConfigurationCache.class);
    private final SocialSecretCipher cipher = new SocialSecretCipher(Base64.getEncoder().encodeToString(new byte[32]));
    private final ExternalAuthConfigurationServiceImpl service = new ExternalAuthConfigurationServiceImpl(providers, registrations, cache, cipher);
    private SysAuthProvider provider;
    private SysAuthRegistration registration;
    private ExternalAuthRuntimeStamp stamp;

    @BeforeEach
    void seed() {
        provider = new SysAuthProvider(); provider.setId(1L); provider.setVersion(3L); provider.setProviderKey("corporate");
        provider.setProtocol("OIDC"); provider.setIssuer("https://identity.example"); provider.setName("公司账户");
        provider.setOptionsJson("{\"tenantId\":\"one\",\"serverUrl\":\"https://public.example\"}");
        registration = new SysAuthRegistration(); registration.setId(2L); registration.setVersion(4L); registration.setProviderId(1L);
        registration.setBusinessClientId("home-client"); registration.setExternalClientId("rp-home");
        registration.setFirstLoginPolicy("BIND_ONLY"); registration.setScopesJson("[\"openid\",\"profile\"]");
        registration.setOptionsJson("{\"tenantId\":\"two\"}");
        registration.setClientSecretCiphertext(cipher.encrypt("registration:2", "private-client-secret"));
        stamp = new ExternalAuthRuntimeStamp(); stamp.setId(2L); stamp.setVersion(4L); stamp.setProviderId(1L); stamp.setProviderVersion(3L);
    }

    @Test
    void databaseDisabledConfigurationRejectsEvenWithAFormerlyValidCache() {
        assertThatThrownBy(() -> service.require("corporate", "home-client")).isInstanceOf(ServiceException.class);
        verifyNoInteractions(cache);
    }

    @Test
    void cacheMissLoadsEncryptedSnapshotAndOnlyReturnedServerValueIsDecrypted() {
        current();
        when(providers.selectById(1L)).thenReturn(provider); when(registrations.selectById(2L)).thenReturn(registration);
        var result = service.require("corporate", "home-client");
        assertThat(result.clientSecret()).isEqualTo("private-client-secret");
        assertThat(result.scopes()).containsExactly("openid", "profile");
        assertThat(result.options()).containsEntry("tenantId", "two").containsEntry("serverUrl", "https://public.example");
        assertThat(result.toString()).doesNotContain("private-client-secret");
        verify(cache).put(eq(ExternalAuthConfigurationCache.key(2, 4, 3)), argThat(snapshot ->
            snapshot.registration().getClientSecretCiphertext().startsWith("v1.")
                && !snapshot.registration().getClientSecretCiphertext().contains("private-client-secret")));
        assertThatThrownBy(() -> result.scopes().add("admin")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.options().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void cacheHitStillRechecksDatabaseAndAvoidsHeavyPayloadQueries() {
        current(); when(cache.get(anyString())).thenReturn(new ExternalAuthConfigurationCache.Snapshot(provider, registration));
        assertThat(service.require("corporate", "home-client").externalClientId()).isEqualTo("rp-home");
        verify(registrations).selectCurrentStamp(2L); verify(providers, never()).selectById(anyLong());
        verify(registrations, never()).selectById(anyLong());
    }

    @Test
    void versionChangeBetweenLookupAndReturnRejectsStaleSnapshot() {
        current(); when(cache.get(anyString())).thenReturn(new ExternalAuthConfigurationCache.Snapshot(provider, registration));
        when(registrations.selectCurrentStamp(2L)).thenReturn(null);
        assertThatThrownBy(() -> service.require("corporate", "home-client")).isInstanceOf(ServiceException.class);
    }

    @Test
    void wrongCachedVersionCannotSatisfyCurrentDatabaseStamp() {
        current(); provider.setVersion(2L);
        when(cache.get(anyString())).thenReturn(new ExternalAuthConfigurationCache.Snapshot(provider, registration));
        assertThatThrownBy(() -> service.require("corporate", "home-client")).isInstanceOf(ServiceException.class);
        verify(providers).selectById(1L);
    }

    @Test
    void bothVersionsAndEnabledStatusAreRequiredForCallback() {
        when(registrations.selectCurrentStamp(2L)).thenReturn(stamp);
        assertThat(service.isCurrent(2, 4, 3)).isTrue();
        assertThat(service.isCurrent(2, 3, 3)).isFalse(); assertThat(service.isCurrent(2, 4, 2)).isFalse();
        when(registrations.selectCurrentStamp(2L)).thenReturn(null);
        assertThat(service.isCurrent(2, 4, 3)).isFalse();
    }

    @Test
    void logoutReadsTombstonesWithoutReopeningLogin() {
        provider.setEnabled(false); registration.setEnabled(false); registration.setDelFlag("1");
        when(registrations.selectForLogout(2L)).thenReturn(registration); when(providers.selectForLogout(1L)).thenReturn(provider);
        assertThat(service.requireForLogout(2).clientSecret()).isEqualTo("private-client-secret");
        assertThatThrownBy(() -> service.require("corporate", "home-client")).isInstanceOf(ServiceException.class);
        verifyNoInteractions(cache);
    }

    @Test
    void emptyPublicClientIdDoesNotQueryAnything() {
        assertThat(service.listEnabled(" ")).isEmpty(); verifyNoInteractions(registrations);
    }

    private void current() {
        when(registrations.selectEnabledStamp("corporate", "home-client")).thenReturn(stamp);
        when(registrations.selectCurrentStamp(2L)).thenReturn(stamp);
    }
}
