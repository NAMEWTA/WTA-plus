package org.namewta.oidc.usecase;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.service.OidcKeyMaterialService;
import org.springframework.stereotype.Service;

/** 协议密钥适配器只通过用例读取服务端材料，不存在HTTP明文读取入口。 */
@Service
@RequiredArgsConstructor
public class OidcKeyMaterialUseCase {
    private final OidcKeyMaterialService service;

    /** 返回当前用途的活动键版本。 */
    public String activeKid(String kind) {
        return service.activeKid(kind);
    }

    /** 解封指定用途和版本的密钥。 */
    public String material(String kind, String kid) {
        return service.material(kind, kid);
    }

    /** 公开集合包含保留版本的公钥。 */
    public JWKSet publicKeys() {
        return service.publicKeys();
    }

    /** 返回唯一活动签名私钥，只用于服务端签发。 */
    public RSAKey signingKey() {
        return service.signingKey();
    }
}
