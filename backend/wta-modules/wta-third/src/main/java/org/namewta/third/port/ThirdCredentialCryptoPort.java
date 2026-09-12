package org.namewta.third.port;

import org.namewta.third.domain.ThirdCredential;

/** Authenticated credential encryption boundary. */
public interface ThirdCredentialCryptoPort {
    EncryptedSecret encrypt(String scopeType, String credentialType, String json);

    String decrypt(ThirdCredential credential);

    record EncryptedSecret(byte[] ciphertext, byte[] nonce, byte[] authTag) {
    }
}
