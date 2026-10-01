package org.namewta.oidc.adapter.provider;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.support.OidcUriPolicy;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** 持久 JWK 文件与独立 AES-GCM 状态密钥；重启不产生替代密钥。 */
@Service
public class OidcKeyAdapter implements org.namewta.oidc.port.OidcKeyPort {
    private final OidcProperties properties;
    private final org.namewta.oidc.port.OidcKeyMaterialPort materialStore;
    private final SecureRandom random = new SecureRandom();

    /** 组装当前协议能力所需的明确依赖。 */
    public OidcKeyAdapter(OidcProperties properties) {
        this.properties = properties;
        this.materialStore = null;
    }

    /** 生产使用数据库持久密钥，文件构造器仅保留显式旧Java装配兼容。 */
    @org.springframework.beans.factory.annotation.Autowired
    public OidcKeyAdapter(
            OidcProperties properties, org.namewta.oidc.port.OidcKeyMaterialPort materialStore) {
        this.properties = properties;
        this.materialStore = materialStore;
    }

    /** 读取持久 JWK 集合，不在缺失时生成替代材料。 */
    public JWKSet keys() {
        if (materialStore != null) return materialStore.publicKeys();
        if (properties.getJwkSetFile() == null || properties.getJwkSetFile().isBlank())
            throw new IllegalStateException("OIDC keys unavailable");
        try {
            return JWKSet.parse(Files.readString(Path.of(properties.getJwkSetFile())));
        } catch (java.io.IOException | java.text.ParseException failure) {
            throw new IllegalStateException("OIDC keys unavailable", failure);
        }
    }

    /** 读取配置指定的私有签名键并验证算法与长度。 */
    public RSAKey active() {
        if (materialStore != null) return materialStore.signingKey();
        var key = keys().getKeyByKeyId(properties.getActiveKid());
        if (!(key instanceof RSAKey rsa) || !rsa.isPrivate() || rsa.size() < 2048)
            throw new IllegalStateException("OIDC signing key unavailable");
        return rsa;
    }

    /** 检查固定发行方、持久签名键与状态密钥是否可用。 */
    public boolean ready() {
        try {
            OidcUriPolicy.issuer(properties.getIssuer(), properties.isAllowHttp());
            OidcUriPolicy.validate(properties.getSsoWebUrl(), properties.isAllowHttp());
            if (!properties
                    .getIssuer()
                    .equals(
                            OidcUriPolicy.origin(
                                    properties.getSsoWebUrl(), properties.isAllowHttp())))
                return false;
            active();
            stateKey();
            return properties.getCodeTtlSeconds() > 0 && properties.getAccessTtlSeconds() > 0;
        } catch (RuntimeException failure) {
            return false;
        }
    }

    /** 要求部署独立提供精确 256 位状态密钥，不生成隐式替代密钥。 */
    private SecretKey stateKey() {
        return stateKey(materialStore == null ? null : materialStore.activeKid("STATE"));
    }

    /** 从密文指定版本解封状态键，轮换不破坏旧授权。 */
    private SecretKey stateKey(String kid) {
        try {
            byte[] bytes =
                    Base64.getDecoder()
                            .decode(
                                    materialStore == null
                                            ? properties.getStateEncryptionKey()
                                            : materialStore.material("STATE", kid));
            if (bytes.length != 32) throw new IllegalArgumentException();
            return new SecretKeySpec(bytes, "AES");
        } catch (RuntimeException e) {
            throw new IllegalStateException("OIDC state key unavailable");
        }
    }

    /** 以独立随机 nonce 和用途绑定的 AAD 认证加密状态。 */
    public String encrypt(String purpose, String plaintext) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            String kid = materialStore == null ? null : materialStore.activeKid("STATE");
            cipher.init(Cipher.ENCRYPT_MODE, stateKey(kid), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(purpose));
            return (kid == null ? "" : "v2." + kid + ".")
                    + Base64.getEncoder().encodeToString(nonce)
                    + "."
                    + Base64.getEncoder()
                            .encodeToString(
                                    cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("OIDC state unavailable", e);
        }
    }

    /** 校验用途、发行方和认证标签后解密状态，失败时拒绝使用。 */
    public String decrypt(String purpose, String envelope) {
        try {
            String[] parts = envelope.split("\\.", -1);
            String kid = null;
            if (parts.length == 4 && "v2".equals(parts[0])) {
                kid = parts[1];
                parts = new String[] {parts[2], parts[3]};
            } else if (parts.length != 2 || materialStore != null)
                throw new IllegalArgumentException();
            byte[] nonce = Base64.getDecoder().decode(parts[0]);
            if (nonce.length != 12) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, stateKey(kid), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad(purpose));
            return new String(
                    cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("OIDC state unavailable", e);
        }
    }

    /** 将认证密文绑定到固定发行方和唯一用途，防止跨授权替换。 */
    private byte[] aad(String purpose) {
        return ("wta-oidc:v1:" + properties.getIssuer() + ":" + purpose)
                .getBytes(StandardCharsets.UTF_8);
    }
}
