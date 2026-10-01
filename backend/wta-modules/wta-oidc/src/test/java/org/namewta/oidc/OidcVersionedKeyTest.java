package org.namewta.oidc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.namewta.oidc.adapter.provider.OidcKeyAdapter;
import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.port.OidcKeyMaterialPort;

import java.util.Base64;

@Tag("dev")
class OidcVersionedKeyTest {
    @Test
    void rotationReadsOldStateVersionButRejectsWrongPurposeAndUnknownVersion() {
        var properties = new OidcProperties();
        properties.setIssuer("https://sso.example");
        var materials = mock(OidcKeyMaterialPort.class);
        when(materials.activeKid("STATE")).thenReturn("old");
        when(materials.material("STATE", "old"))
                .thenReturn(Base64.getEncoder().encodeToString(new byte[32]));
        byte[] newBytes = new byte[32];
        newBytes[0] = 1;
        when(materials.material("STATE", "new"))
                .thenReturn(Base64.getEncoder().encodeToString(newBytes));
        var adapter = new OidcKeyAdapter(properties, materials);
        String old = adapter.encrypt("grant:1", "sensitive");
        assertThat(old).startsWith("v2.old.").doesNotContain("sensitive");
        when(materials.activeKid("STATE")).thenReturn("new");
        String latest = adapter.encrypt("grant:1", "updated");
        assertThat(latest).startsWith("v2.new.");
        assertThat(adapter.decrypt("grant:1", old)).isEqualTo("sensitive");
        assertThat(adapter.decrypt("grant:1", latest)).isEqualTo("updated");
        assertThatThrownBy(() -> adapter.decrypt("grant:2", old))
                .isInstanceOf(IllegalStateException.class);
        when(materials.material("STATE", "missing"))
                .thenThrow(new IllegalStateException("missing"));
        assertThatThrownBy(() -> adapter.decrypt("grant:1", old.replace("v2.old.", "v2.missing.")))
                .isInstanceOf(IllegalStateException.class);
    }
}
