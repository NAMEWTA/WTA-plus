package org.namewta.oidc.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.social.crypto.SocialSecretCipher;
import org.namewta.oidc.dao.OidcKeyMaterialDao;
import org.namewta.oidc.domain.OidcKeyMaterial;
import org.namewta.oidc.domain.vo.OidcKeyVo;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** 密钥管理规则，历史版本永不被轮换操作覆盖。 */
@Service
@RequiredArgsConstructor
public class OidcKeyMaterialService {
    private final OidcKeyMaterialDao dao;
    private final SocialSecretCipher cipher;

    /** 返回脱敏目录。 */
    public List<OidcKeyVo> list() {
        return dao.list().stream()
                .map(
                        k ->
                                new OidcKeyVo(
                                        k.getKid(),
                                        k.getKind(),
                                        Boolean.TRUE.equals(k.getActive()),
                                        k.getCreateTime()))
                .toList();
    }

    /** 使用系统随机源创建并激活一个新版本。 */
    public void generate(String kind) {
        try {
            if ("SIGNING".equals(kind)) {
                importKey(kind, new RSAKeyGenerator(2048).generate().toJSONString());
                return;
            }
            if (!"STATE".equals(kind)) throw new ServiceException("未知密钥用途");
            byte[] value = new byte[32];
            new SecureRandom().nextBytes(value);
            importKey(kind, Base64.getEncoder().encodeToString(value));
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new IllegalStateException("无法生成签名键", e);
        }
    }

    /** 校验导入材料，以根密钥封装后保留为独立版本。 */
    public void importKey(String kind, String material) {
        if (material == null || material.isBlank()) throw new ServiceException("请提供密钥材料");
        String kid = Long.toString(IdGeneratorUtil.nextLongId());
        try {
            if ("SIGNING".equals(kind)) {
                var rsa = RSAKey.parse(material);
                if (!rsa.isPrivate() || rsa.size() < 2048)
                    throw new ServiceException("需要至少2048位RSA私钥");
                material =
                        new RSAKey.Builder(rsa)
                                .keyID(kid)
                                .keyUse(KeyUse.SIGNATURE)
                                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                                .build()
                                .toJSONString();
            } else if ("STATE".equals(kind)) {
                if (Base64.getDecoder().decode(material).length != 32)
                    throw new ServiceException("状态密钥需要32字节Base64材料");
            } else throw new ServiceException("未知密钥用途");
        } catch (java.text.ParseException | IllegalArgumentException e) {
            throw new ServiceException("密钥材料无效");
        }
        var row = new OidcKeyMaterial();
        row.setKeyMaterialId(Long.valueOf(kid));
        row.setKid(kid);
        row.setKind(kind);
        row.setEncryptedMaterial(cipher.encrypt("oidc-key:" + kind + ":" + kid, material));
        row.setActive(true);
        row.setVersion(0);
        row.setDelFlag("0");
        dao.deactivate(kind);
        dao.insert(row);
    }

    /** 读取活动版本；无密钥时停止签发，不自动生成替代键。 */
    public String activeKid(String kind) {
        var k = dao.active(kind);
        if (k == null) throw new IllegalStateException("OIDC密钥尚未配置");
        return k.getKid();
    }

    /** 只在服务端解封指定用途/版本的材料。 */
    public String material(String kind, String kid) {
        var k = dao.find(kind, kid);
        if (k == null) throw new IllegalStateException("OIDC密钥版本不存在");
        return cipher.decrypt("oidc-key:" + kind + ":" + kid, k.getEncryptedMaterial());
    }

    /** 公钥集合包括历史版本，保障轮换期间已签凭据可验证。 */
    public JWKSet publicKeys() {
        try {
            var keys = new ArrayList<JWK>();
            for (var k : dao.list())
                if ("SIGNING".equals(k.getKind()))
                    keys.add(RSAKey.parse(material(k.getKind(), k.getKid())).toPublicJWK());
            return new JWKSet(keys);
        } catch (java.text.ParseException e) {
            throw new IllegalStateException("OIDC签名材料损坏", e);
        }
    }

    /** 返回当前私有签名键。 */
    public RSAKey signingKey() {
        try {
            return RSAKey.parse(material("SIGNING", activeKid("SIGNING")));
        } catch (java.text.ParseException e) {
            throw new IllegalStateException("OIDC签名材料损坏", e);
        }
    }

    /** 就绪状态不回显密钥或底层异常。 */
    public boolean ready(String kind) {
        try {
            material(kind, activeKid(kind));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
