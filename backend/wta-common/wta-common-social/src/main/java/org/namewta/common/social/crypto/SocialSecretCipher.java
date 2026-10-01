package org.namewta.common.social.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** 外部认证凭据和短时状态的 AES-256-GCM 加密；purpose 作为 AAD 隔离不同用途。 */
public final class SocialSecretCipher {
    private static final String VERSION = "v1";
    private static final int NONCE_LENGTH = 12;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    /** 接收标准 Base64 的 32 字节根密钥；未配置时允许非外部认证功能启动，使用时明确失败。 */
    public SocialSecretCipher(String rootKey) {
        if (rootKey == null || rootKey.isBlank()) {
            key = null;
            return;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(rootKey.strip());
            if (decoded.length != 32) throw new IllegalArgumentException("invalid key length");
            key = new SecretKeySpec(decoded, "AES");
            java.util.Arrays.fill(decoded, (byte) 0);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("AUTH_CONFIG_ROOT_KEY 必须为 Base64 编码的 32 字节密钥");
        }
    }

    /** 加密并生成版本化密文；每次调用使用独立 nonce，不记录明文。 */
    public String encrypt(String purpose, String plaintext) {
        check(purpose);
        if (plaintext == null || plaintext.length() > 65536) throw new IllegalArgumentException("外部认证明文无效");
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, purpose, nonce);
            return VERSION + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("外部认证凭据加密失败", exception);
        }
    }

    /** 解密相同用途的 v1 密文；修改内容、用途或根密钥都会失败，不支持明文回退。 */
    public String decrypt(String purpose, String ciphertext) {
        check(purpose);
        if (ciphertext == null || ciphertext.length() > 350000) throw new IllegalArgumentException("外部认证密文无效");
        try {
            String[] parts = ciphertext.split("\\.", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0])) throw new IllegalArgumentException("invalid envelope");
            byte[] nonce = Base64.getUrlDecoder().decode(parts[1]);
            if (nonce.length != NONCE_LENGTH) throw new IllegalArgumentException("invalid nonce");
            byte[] encrypted = Base64.getUrlDecoder().decode(parts[2]);
            return new String(cipher(Cipher.DECRYPT_MODE, purpose, nonce).doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("外部认证密文无效或根密钥不匹配");
        }
    }

    private void check(String purpose) {
        if (key == null) throw new IllegalStateException("外部认证功能需要 AUTH_CONFIG_ROOT_KEY");
        if (purpose == null || purpose.isBlank() || purpose.length() > 200) throw new IllegalArgumentException("外部认证密文用途无效");
    }

    private Cipher cipher(int mode, String purpose, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(("namewta:external-auth:" + VERSION + ":" + purpose).getBytes(StandardCharsets.UTF_8));
        return cipher;
    }
}
