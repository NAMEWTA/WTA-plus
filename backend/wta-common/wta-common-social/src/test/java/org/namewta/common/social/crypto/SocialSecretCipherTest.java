package org.namewta.common.social.crypto;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

/** 凭据密文的实际加解密与完整性边界。 */
@Tag("dev")
class SocialSecretCipherTest {
    private final SocialSecretCipher cipher = new SocialSecretCipher(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void roundTripUsesRandomNonceAndDoesNotContainPlaintext() {
        String first = cipher.encrypt("registration:1", "credential-中文");
        String second = cipher.encrypt("registration:1", "credential-中文");
        assertThat(first).startsWith("v1.").doesNotContain("credential").isNotEqualTo(second);
        assertThat(cipher.decrypt("registration:1", first)).isEqualTo("credential-中文");
        assertThat(cipher.decrypt("registration:1", second)).isEqualTo("credential-中文");
    }

    @Test
    void changingPurposeOrKeyCannotReadCiphertext() {
        String encrypted = cipher.encrypt("registration:1", "private-value");
        assertThatThrownBy(() -> cipher.decrypt("registration:2", encrypted)).isInstanceOf(IllegalArgumentException.class);
        byte[] otherKey = new byte[32]; otherKey[0] = 1;
        var other = new SocialSecretCipher(Base64.getEncoder().encodeToString(otherKey));
        assertThatThrownBy(() -> other.decrypt("registration:1", encrypted)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tamperedTruncatedAndUnknownVersionEnvelopesFailClosed() {
        String encrypted = cipher.encrypt("state", "private-value");
        String[] pieces = encrypted.split("\\.");
        byte[] bytes = Base64.getUrlDecoder().decode(pieces[2]); bytes[0] ^= 1;
        String tampered = pieces[0] + "." + pieces[1] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        for (String invalid : new String[]{tampered, encrypted.substring(0, 15), encrypted.replace("v1.", "v2."), "private-value"}) {
            assertThatThrownBy(() -> cipher.decrypt("state", invalid)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("private-value");
        }
    }

    @Test
    void missingKeyOnlyBlocksTheFeatureAndMalformedSpecifiedKeyFailsEarly() {
        var absent = new SocialSecretCipher("");
        assertThatThrownBy(() -> absent.encrypt("x", "private")).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AUTH_CONFIG_ROOT_KEY");
        assertThatThrownBy(() -> new SocialSecretCipher("bad")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SocialSecretCipher(Base64.getEncoder().encodeToString(new byte[16])))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyPurposeAndNullPlaintext() {
        assertThatThrownBy(() -> cipher.encrypt("", "value")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cipher.encrypt("state", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
