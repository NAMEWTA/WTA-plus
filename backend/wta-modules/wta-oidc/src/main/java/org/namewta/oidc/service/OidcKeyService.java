package org.namewta.oidc.service;

import com.nimbusds.jose.jwk.*;

import lombok.RequiredArgsConstructor;

import org.namewta.oidc.port.OidcKeyPort;
import org.springframework.stereotype.Service;

/** 用例访问签名材料与状态保护的服务入口。 */
@Service
@RequiredArgsConstructor
public class OidcKeyService {
    private final OidcKeyPort port;

    /** 读取持久 JWK 集合，不在缺失时生成替代材料。 */
    public JWKSet keys() {
        return port.keys();
    }

    /** 读取配置指定的私有签名键并验证算法与长度。 */
    public RSAKey active() {
        return port.active();
    }

    /** 检查固定发行方、持久签名键与状态密钥是否可用。 */
    public boolean ready() {
        return port.ready();
    }

    /** 以独立随机 nonce 和用途绑定的 AAD 认证加密状态。 */
    public String encrypt(String p, String v) {
        return port.encrypt(p, v);
    }

    /** 校验用途、发行方和认证标签后解密状态，失败时拒绝使用。 */
    public String decrypt(String p, String v) {
        return port.decrypt(p, v);
    }
}
