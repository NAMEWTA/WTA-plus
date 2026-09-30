package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.namewta.oidc.adapter.provider.OidcKeyAdapter;
import org.namewta.oidc.config.OidcProperties;

import java.util.Base64;

@Tag("dev")
class OidcKeyAdapterTest {
    @Test
    void authenticatedStateCannotMoveAcrossPurposeOrIssuer() {
        var p = new OidcProperties();
        p.setIssuer("https://sso.example");
        p.setStateEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
        var keys = new OidcKeyAdapter(p);
        String value = keys.encrypt("grant:a", "secret-token");
        assertThat(value).doesNotContain("secret-token");
        assertThat(keys.decrypt("grant:a", value)).isEqualTo("secret-token");
        assertThatThrownBy(() -> keys.decrypt("grant:b", value))
                .isInstanceOf(IllegalStateException.class);
        p.setIssuer("https://other.example");
        assertThatThrownBy(() -> keys.decrypt("grant:a", value))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingPersistentKeyNeverCreatesReplacement() {
        var p = new OidcProperties();
        var keys = new OidcKeyAdapter(p);
        assertThat(keys.ready()).isFalse();
        assertThatThrownBy(keys::active).isInstanceOf(IllegalStateException.class);
    }
}
