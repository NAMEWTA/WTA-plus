package org.namewta.third.adapter.security;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.third.config.ThirdCryptoProperties;
import org.namewta.third.domain.ThirdCredential;
import org.namewta.third.port.ThirdCredentialCryptoPort;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class ThirdCredentialCryptoAdapter implements ThirdCredentialCryptoPort {
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int TAG_BYTES = TAG_BITS / 8;
    private final ThirdCryptoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedSecret encrypt(String scopeType, String credentialType, String json) {
        if (json == null || json.isBlank()) throw new ServiceException("凭据内容不能为空");
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce, aad(scopeType, credentialType));
            byte[] combined = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
            int split = combined.length - TAG_BYTES;
            return new EncryptedSecret(java.util.Arrays.copyOf(combined, split), nonce,
                java.util.Arrays.copyOfRange(combined, split, combined.length));
        } catch (GeneralSecurityException e) {
            throw new ServiceException("凭据加密失败");
        }
    }

    public String decrypt(ThirdCredential credential) {
        if (credential == null || credential.getCiphertext() == null || credential.getNonce() == null
            || credential.getAuthTag() == null || !"v1".equals(credential.getKekVersion())
            || credential.getNonce().length != NONCE_BYTES || credential.getAuthTag().length != TAG_BYTES
            || credential.getCiphertext().length == 0) throw new ServiceException("凭据不可用");
        byte[] combined = new byte[credential.getCiphertext().length + credential.getAuthTag().length];
        System.arraycopy(credential.getCiphertext(), 0, combined, 0, credential.getCiphertext().length);
        System.arraycopy(credential.getAuthTag(), 0, combined, credential.getCiphertext().length, credential.getAuthTag().length);
        try {
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, credential.getNonce(), aad(credential.getScopeType(), credential.getCredentialType()));
            return new String(cipher.doFinal(combined), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new ServiceException("凭据解密失败");
        }
    }

    public byte[] masterKey() {
        String raw = properties.getMasterKey();
        if (raw == null || raw.isBlank()) throw new ServiceException("第三方凭据主密钥未配置");
        byte[] key;
        try {
            key = Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException ignored) {
            key = raw.getBytes(StandardCharsets.UTF_8);
        }
        if (key.length != 16 && key.length != 24 && key.length != 32) key = raw.getBytes(StandardCharsets.UTF_8);
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new ServiceException("第三方凭据主密钥长度必须为 16、24 或 32 字节");
        }
        return key;
    }

    private Cipher cipher(int mode, byte[] nonce, byte[] aad) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(masterKey(), "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher;
    }

    private static byte[] aad(String scopeType, String credentialType) {
        return (scopeType + ":" + credentialType).getBytes(StandardCharsets.UTF_8);
    }

}
