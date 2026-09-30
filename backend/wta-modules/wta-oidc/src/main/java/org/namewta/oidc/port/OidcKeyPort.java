package org.namewta.oidc.port;

import com.nimbusds.jose.jwk.*;

/** 持久签名材料与认证加密的基础设施端口。 */
public interface OidcKeyPort {
    /** 读取持久 JWK 集合，不在缺失时生成替代材料。 */
    JWKSet keys();

    /** 读取配置指定的私有签名键并验证算法与长度。 */
    RSAKey active();

    /** 检查固定发行方、持久签名键与状态密钥是否可用。 */
    boolean ready();

    /** 以独立随机 nonce 和用途绑定的 AAD 认证加密状态。 */
    String encrypt(String purpose, String plaintext);

    /** 校验用途、发行方和认证标签后解密状态，失败时拒绝使用。 */
    String decrypt(String purpose, String envelope);
}
