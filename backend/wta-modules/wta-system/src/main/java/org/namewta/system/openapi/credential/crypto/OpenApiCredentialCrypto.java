package org.namewta.system.openapi.credential.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM credential encryption with owner/app-key/version-bound AAD.
 */
@Component
@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")
public class OpenApiCredentialCrypto {

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BYTES = 16;
    private static final int APP_KEY_BYTES = 16;
    private static final int APP_SECRET_BYTES = 32;
    private static final byte[] CONTEXT = "NAMEWTA-OPENAPI-CREDENTIAL-V1".getBytes(StandardCharsets.US_ASCII);

    private final OpenApiKekProvider kekProvider;
    private final SecureRandom secureRandom;

    @Autowired
    public OpenApiCredentialCrypto(OpenApiKekProvider kekProvider) {
        this(kekProvider, new SecureRandom());
    }

    public OpenApiCredentialCrypto(OpenApiKekProvider kekProvider, SecureRandom secureRandom) {
        this.kekProvider = kekProvider;
        this.secureRandom = secureRandom;
    }

    public GeneratedCredential generate(Long ownerUserId) {
        String appKey = randomUrlToken(APP_KEY_BYTES);
        String appSecret = generateSecret();
        return new GeneratedCredential(appKey, appSecret, encrypt(ownerUserId, appKey, appSecret));
    }

    public String generateSecret() {
        return randomUrlToken(APP_SECRET_BYTES);
    }

    public EncryptedSecret encrypt(Long ownerUserId, String appKey, String appSecret) {
        String version = null;
        byte[] key = null;
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        byte[] plaintext = appSecret.getBytes(StandardCharsets.UTF_8);
        try {
            version = kekProvider.activeVersion();
            key = kekProvider.keyForVersion(version);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, key, nonce, aad(ownerUserId, appKey, version));
            byte[] cipherAndTag = cipher.doFinal(plaintext);
            int cipherLength = cipherAndTag.length - TAG_BYTES;
            return new EncryptedSecret(Arrays.copyOf(cipherAndTag, cipherLength), nonce,
                Arrays.copyOfRange(cipherAndTag, cipherLength, cipherAndTag.length), version);
        } catch (GeneralSecurityException | RuntimeException e) {
            throw failure(e);
        } finally {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    public String decrypt(Long ownerUserId, String appKey, EncryptedSecret encrypted) {
        byte[] key = null;
        byte[] cipherAndTag = ByteBuffer.allocate(encrypted.ciphertext().length + encrypted.tag().length)
            .put(encrypted.ciphertext()).put(encrypted.tag()).array();
        byte[] plaintext = null;
        try {
            key = kekProvider.keyForVersion(encrypted.kekVersion());
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, key, encrypted.nonce(),
                aad(ownerUserId, appKey, encrypted.kekVersion()));
            plaintext = cipher.doFinal(cipherAndTag);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException e) {
            throw failure(e);
        } finally {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            Arrays.fill(cipherAndTag, (byte) 0);
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }

    private String randomUrlToken(int bytes) {
        byte[] raw = new byte[bytes];
        secureRandom.nextBytes(raw);
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } finally {
            Arrays.fill(raw, (byte) 0);
        }
    }

    private static Cipher cipher(int mode, byte[] key, byte[] nonce, byte[] aad)
        throws GeneralSecurityException {
        if (key.length != 32 || nonce.length != NONCE_BYTES) {
            throw new OpenApiCredentialCryptoException();
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BYTES * 8, nonce));
        cipher.updateAAD(aad);
        return cipher;
    }

    private static byte[] aad(Long ownerUserId, String appKey, String version) {
        if (ownerUserId == null || appKey == null || version == null) {
            throw new OpenApiCredentialCryptoException();
        }
        byte[] appKeyBytes = appKey.getBytes(StandardCharsets.UTF_8);
        byte[] versionBytes = version.getBytes(StandardCharsets.US_ASCII);
        return ByteBuffer.allocate(CONTEXT.length + Long.BYTES + appKeyBytes.length + versionBytes.length + 2)
            .put(CONTEXT).putLong(ownerUserId).put((byte) 0).put(appKeyBytes).put((byte) 0)
            .put(versionBytes).array();
    }

    private static OpenApiCredentialCryptoException failure(Throwable cause) {
        return cause instanceof OpenApiCredentialCryptoException cryptoException
            ? cryptoException : new OpenApiCredentialCryptoException(cause);
    }

    public record EncryptedSecret(byte[] ciphertext, byte[] nonce, byte[] tag, String kekVersion) {
    }

    public record GeneratedCredential(String appKey, String appSecret, EncryptedSecret encrypted) {
    }
}
