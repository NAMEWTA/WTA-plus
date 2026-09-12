package org.namewta.third.service;

import org.namewta.third.config.ThirdCryptoProperties;
import org.namewta.third.domain.ThirdCredential;
import org.namewta.third.adapter.security.ThirdCredentialCryptoAdapter;
import org.namewta.third.port.ThirdCredentialCryptoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@Tag("local")
class ThirdCredentialCryptoTest {
    @Test
    void encryptsAndDecryptsWithAuthenticatedCiphertext() {
        ThirdCryptoProperties properties = new ThirdCryptoProperties();
        properties.setMasterKey(Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));
        ThirdCredentialCryptoAdapter crypto = new ThirdCredentialCryptoAdapter(properties);
        ThirdCredentialCryptoPort.EncryptedSecret encrypted = crypto.encrypt("PROVIDER", "API_KEY", "{\"headers\":{\"X-Api-Key\":\"secret\"}}");
        ThirdCredential credential = new ThirdCredential();
        credential.setScopeType("PROVIDER"); credential.setCredentialType("API_KEY");
        credential.setKekVersion("v1");
        credential.setCiphertext(encrypted.ciphertext()); credential.setNonce(encrypted.nonce()); credential.setAuthTag(encrypted.authTag());
        assertEquals("{\"headers\":{\"X-Api-Key\":\"secret\"}}", crypto.decrypt(credential));
        credential.getAuthTag()[0] ^= 1;
        assertThrows(RuntimeException.class, () -> crypto.decrypt(credential));
    }

    @Test
    void rejectsUnknownKeyVersion() {
        ThirdCryptoProperties properties = new ThirdCryptoProperties();
        properties.setMasterKey(Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));
        ThirdCredentialCryptoAdapter crypto = new ThirdCredentialCryptoAdapter(properties);
        ThirdCredential credential = new ThirdCredential();
        credential.setScopeType("PROVIDER"); credential.setCredentialType("API_KEY"); credential.setKekVersion("v2");
        credential.setCiphertext(new byte[] {1}); credential.setNonce(new byte[12]); credential.setAuthTag(new byte[16]);
        assertThrows(RuntimeException.class, () -> crypto.decrypt(credential));
    }

    @Test
    void rejectsMissingMasterKey() {
        ThirdCryptoProperties properties = new ThirdCryptoProperties();
        ThirdCredentialCryptoAdapter crypto = new ThirdCredentialCryptoAdapter(properties);
        assertThrows(RuntimeException.class, () -> crypto.encrypt("PROVIDER", "API_KEY", "{}"));
    }
}
