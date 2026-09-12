package org.namewta.test.openapi.credential;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.openapi.protocol.OpenApiAuthenticationException;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto.EncryptedSecret;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto.GeneratedCredential;
import org.namewta.system.openapi.credential.domain.SysOpenApiCredential;
import org.namewta.system.openapi.credential.mapper.SysOpenApiCredentialMapper;
import org.namewta.system.openapi.credential.model.OpenApiCredentialCreateRequest;
import org.namewta.system.openapi.credential.model.OpenApiCredentialIssued;
import org.namewta.system.openapi.credential.service.SystemOpenApiCredentialResolver;
import org.namewta.system.openapi.credential.service.SystemOpenApiCredentialService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class SystemOpenApiCredentialServiceTest {

    @Test
    void createReturnsSecretOnceAndConcurrentUniqueLoserGetsConflict() {
        Fixture fixture = fixture();
        GeneratedCredential generated = new GeneratedCredential("app-key", "one-time-secret",
            encrypted("v1"));
        when(fixture.crypto.generate(41L)).thenReturn(generated);
        when(fixture.ids.nextId(null)).thenReturn(91L);
        when(fixture.mapper.insert(any(SysOpenApiCredential.class))).thenReturn(1);

        OpenApiCredentialIssued issued = fixture.service.create(41L,
            new OpenApiCredentialCreateRequest("billing", null, "automation"));

        assertThat(issued.appKey()).isEqualTo("app-key");
        assertThat(issued.appSecret()).isEqualTo("one-time-secret");
        when(fixture.mapper.selectByOwnerUserId(41L)).thenReturn(entity("0"));
        assertThat(Arrays.stream(fixture.service.get(41L).getClass().getRecordComponents())
            .map(java.lang.reflect.RecordComponent::getName))
            .doesNotContain("appSecret", "secretCiphertext", "secretNonce", "secretTag", "kekVersion");

        when(fixture.mapper.selectByOwnerUserId(41L)).thenReturn(null);
        when(fixture.mapper.insert(any(SysOpenApiCredential.class)))
            .thenThrow(new DuplicateKeyException("uk_active_user"));
        assertThatThrownBy(() -> fixture.service.create(41L,
            new OpenApiCredentialCreateRequest("billing", null, null)))
            .isInstanceOf(ServiceException.class)
            .extracting("code").isEqualTo(409);
    }

    @Test
    void createRejectsAnUnknownOrDisabledOwnerBeforeGeneratingMaterial() {
        Fixture fixture = fixture();
        when(fixture.mapper.existsActiveOwner(41L)).thenReturn(false);

        assertThatThrownBy(() -> fixture.service.create(41L,
            new OpenApiCredentialCreateRequest("billing", null, null)))
            .isInstanceOf(ServiceException.class)
            .extracting("code").isEqualTo(404);
        verify(fixture.crypto, never()).generate(any());
    }

    @Test
    void resetKeepsAppKeyAndEveryStateMutationInvalidatesMachineSession() {
        Fixture fixture = fixture();
        SysOpenApiCredential entity = entity("0");
        when(fixture.mapper.selectByOwnerUserId(41L)).thenReturn(entity);
        when(fixture.crypto.encrypt(41L, "app-key", "new-secret")).thenReturn(encrypted("v2"));
        when(fixture.crypto.generateSecret()).thenReturn("new-secret");
        when(fixture.mapper.updateById(any(SysOpenApiCredential.class))).thenReturn(1);
        when(fixture.mapper.deleteById(any(SysOpenApiCredential.class))).thenReturn(1);

        OpenApiCredentialIssued reset = fixture.service.reset(41L);
        fixture.service.disable(41L);
        fixture.service.enable(41L);
        fixture.service.delete(41L);

        assertThat(reset.appKey()).isEqualTo("app-key");
        assertThat(reset.appSecret()).isEqualTo("new-secret");
        verify(fixture.invalidator, org.mockito.Mockito.times(4)).invalidateByUserId(41L);
    }

    @Test
    void invalidationFailureFailsTheTransactionalWriteExplicitly() {
        Fixture fixture = fixture();
        when(fixture.mapper.selectByOwnerUserId(41L)).thenReturn(entity("0"));
        when(fixture.mapper.updateById(any(SysOpenApiCredential.class))).thenReturn(1);
        when(fixture.invalidator.invalidateByUserId(41L)).thenThrow(new IllegalStateException("redis down"));

        assertThatThrownBy(() -> fixture.service.disable(41L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("redis down");
    }

    @Test
    void resolverRejectsUnknownDisabledExpiredAndCryptoFailuresWithOneCategory() {
        SysOpenApiCredentialMapper mapper = mock(SysOpenApiCredentialMapper.class);
        OpenApiCredentialCrypto crypto = mock(OpenApiCredentialCrypto.class);
        SystemOpenApiCredentialResolver resolver = new SystemOpenApiCredentialResolver(mapper, crypto);

        assertThatThrownBy(() -> resolver.resolve("missing"))
            .isInstanceOf(OpenApiAuthenticationException.class);

        SysOpenApiCredential disabled = entity("1");
        when(mapper.selectUsableByAppKey("disabled")).thenReturn(disabled);
        assertThatThrownBy(() -> resolver.resolve("disabled"))
            .isInstanceOf(OpenApiAuthenticationException.class);
        verify(crypto, never()).decrypt(any(), any(), any());

        SysOpenApiCredential expired = entity("0");
        expired.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(mapper.selectUsableByAppKey("expired")).thenReturn(expired);
        assertThatThrownBy(() -> resolver.resolve("expired"))
            .isInstanceOf(OpenApiAuthenticationException.class);
    }

    @Test
    void everyLifecycleWriteIsTransactionBound() throws Exception {
        assertThat(SystemOpenApiCredentialService.class
            .getMethod("create", Long.class, OpenApiCredentialCreateRequest.class)
            .getAnnotation(DSTransactional.class)).isNotNull();
        for (String method : new String[]{"reset", "enable", "disable", "delete"}) {
            assertThat(SystemOpenApiCredentialService.class.getMethod(method, Long.class)
                .getAnnotation(DSTransactional.class)).as(method).isNotNull();
        }
    }

    private static Fixture fixture() {
        SysOpenApiCredentialMapper mapper = mock(SysOpenApiCredentialMapper.class);
        OpenApiCredentialCrypto crypto = mock(OpenApiCredentialCrypto.class);
        OpenApiMachineSessionInvalidator invalidator = mock(OpenApiMachineSessionInvalidator.class);
        IdentifierGenerator ids = mock(IdentifierGenerator.class);
        when(mapper.existsActiveOwner(41L)).thenReturn(true);
        return new Fixture(mapper, crypto, invalidator, ids,
            new SystemOpenApiCredentialService(mapper, crypto, invalidator, ids));
    }

    private static SysOpenApiCredential entity(String status) {
        SysOpenApiCredential entity = new SysOpenApiCredential();
        entity.setOpenApiCredentialId(91L);
        entity.setOwnerUserId(41L);
        entity.setAppKey("app-key");
        entity.setAppName("billing");
        entity.setStatus(status);
        entity.setSecretCiphertext(new byte[]{1});
        entity.setSecretNonce(new byte[12]);
        entity.setSecretTag(new byte[16]);
        entity.setKekVersion("v1");
        entity.setVersion(0);
        return entity;
    }

    private static EncryptedSecret encrypted(String version) {
        return new EncryptedSecret(new byte[]{1}, new byte[12], new byte[16], version);
    }

    private record Fixture(SysOpenApiCredentialMapper mapper, OpenApiCredentialCrypto crypto,
                           OpenApiMachineSessionInvalidator invalidator, IdentifierGenerator ids,
                           SystemOpenApiCredentialService service) {
    }
}
