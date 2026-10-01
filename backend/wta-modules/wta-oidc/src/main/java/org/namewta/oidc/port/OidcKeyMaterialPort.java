package org.namewta.oidc.port;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

/** 协议加密适配器需要的版本化材料能力，私钥不会通过HTTP输出。 */
public interface OidcKeyMaterialPort {
    /** 活动用途版本。 */
    String activeKid(String kind);

    /** 用途与版本绑定的明文，仅在服务端消费。 */
    String material(String kind, String kid);

    /** 包含历史版本的公钥集合。 */
    JWKSet publicKeys();

    /** 当前私有签名键。 */
    RSAKey signingKey();
}
