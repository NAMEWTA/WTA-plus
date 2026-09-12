package org.namewta.test.openapi.credential;

import org.namewta.common.openapi.config.properties.OpenApiProperties;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto.EncryptedSecret;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCrypto.GeneratedCredential;
import org.namewta.system.openapi.credential.crypto.OpenApiCredentialCryptoException;
import org.namewta.system.openapi.credential.crypto.OpenApiKekProvider;
import org.namewta.system.openapi.credential.crypto.PropertyOpenApiKekProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
class OpenApiCredentialCryptoTest {

    private static final byte[] KEY_V1 = new byte[32];
    private static final byte[] KEY_V2 = new byte[32];

    static {
        Arrays.fill(KEY_V1, (byte) 1);
        Arrays.fill(KEY_V2, (byte) 2);
    }

    @Test
    void generatesHighEntropyMaterialAndRoundTripsVersionedAesGcm() {
        OpenApiCredentialCrypto crypto = crypto("v2", Map.of("v1", KEY_V1, "v2", KEY_V2));

        GeneratedCredential generated = crypto.generate(41L);

        assertThat(generated.appKey()).hasSizeGreaterThanOrEqualTo(22);
        assertThat(generated.appSecret()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(generated.encrypted().kekVersion()).isEqualTo("v2");
        assertThat(generated.encrypted().nonce()).hasSize(12);
        assertThat(generated.encrypted().tag()).hasSize(16);
        assertThat(crypto.decrypt(41L, generated.appKey(), generated.encrypted()))
            .isEqualTo(generated.appSecret());
    }

    @Test
    void decryptsOldVersionButFailsClosedForTamperWrongOwnerAndMissingKek() {
        OpenApiCredentialCrypto oldCrypto = crypto("v1", Map.of("v1", KEY_V1));
        GeneratedCredential generated = oldCrypto.generate(41L);
        OpenApiCredentialCrypto rotatedCrypto = crypto("v2", Map.of("v1", KEY_V1, "v2", KEY_V2));

        assertThat(rotatedCrypto.decrypt(41L, generated.appKey(), generated.encrypted()))
            .isEqualTo(generated.appSecret());

        byte[] tag = generated.encrypted().tag().clone();
        tag[0] ^= 1;
        EncryptedSecret tampered = new EncryptedSecret(generated.encrypted().ciphertext(),
            generated.encrypted().nonce(), tag, generated.encrypted().kekVersion());
        assertThatThrownBy(() -> rotatedCrypto.decrypt(41L, generated.appKey(), tampered))
            .isInstanceOf(OpenApiCredentialCryptoException.class)
            .hasMessage("OpenAPI credential crypto unavailable");
        assertThatThrownBy(() -> rotatedCrypto.decrypt(42L, generated.appKey(), generated.encrypted()))
            .isInstanceOf(OpenApiCredentialCryptoException.class)
            .hasMessage("OpenAPI credential crypto unavailable");
        assertThatThrownBy(() -> crypto("v2", Map.of("v2", KEY_V2))
            .decrypt(41L, generated.appKey(), generated.encrypted()))
            .isInstanceOf(OpenApiCredentialCryptoException.class)
            .hasMessage("OpenAPI credential crypto unavailable");
    }

    @Test
    void propertyProviderRequiresExactAes256KeyAndSupportsExplicitOldVersions() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.setKekVersion("v2");
        properties.setKek(Base64.getEncoder().encodeToString(KEY_V2));
        MockEnvironment environment = new MockEnvironment()
            .withProperty("openapi.keks.v1", Base64.getEncoder().encodeToString(KEY_V1));
        PropertyOpenApiKekProvider provider = new PropertyOpenApiKekProvider(properties, environment);

        assertThat(provider.activeVersion()).isEqualTo("v2");
        assertThat(provider.keyForVersion("v2")).containsExactly(KEY_V2);
        assertThat(provider.keyForVersion("v1")).containsExactly(KEY_V1);

        properties.setKek(Base64.getEncoder().encodeToString(new byte[16]));
        assertThatThrownBy(() -> provider.keyForVersion("v2"))
            .isInstanceOf(OpenApiCredentialCryptoException.class)
            .hasMessage("OpenAPI credential crypto unavailable");
    }

    private static OpenApiCredentialCrypto crypto(String activeVersion, Map<String, byte[]> keys) {
        OpenApiKekProvider provider = new OpenApiKekProvider() {
            @Override
            public String activeVersion() {
                return activeVersion;
            }

            @Override
            public byte[] keyForVersion(String version) {
                byte[] key = keys.get(version);
                return key == null ? null : key.clone();
            }
        };
        return new OpenApiCredentialCrypto(provider, new SecureRandom());
    }
}
