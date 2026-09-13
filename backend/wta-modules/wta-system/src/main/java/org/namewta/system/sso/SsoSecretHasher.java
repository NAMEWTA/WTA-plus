package org.namewta.system.sso;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.namewta.common.core.utils.StringUtils;

/**
 * SSO 客户端密钥哈希。明文只在签发时出现，存储仅为哈希。
 */
public final class SsoSecretHasher {

    private SsoSecretHasher() {
    }

    /**
     * 生成可交付的明文密钥。
     *
     * @return 随机明文
     */
    public static String generatePlaintext() {
        return RandomUtil.randomString(48);
    }

    /**
     * 计算存储哈希。
     *
     * @param plaintext 明文密钥
     * @return BCrypt 哈希
     */
    public static String hash(String plaintext) {
        if (StringUtils.isBlank(plaintext)) {
            throw new IllegalArgumentException("SSO 密钥不能为空");
        }
        return BCrypt.hashpw(plaintext);
    }

    /**
     * 校验明文是否匹配哈希。
     *
     * @param plaintext 明文
     * @param hash      存储哈希
     * @return 是否匹配
     */
    public static boolean matches(String plaintext, String hash) {
        if (StringUtils.isBlank(plaintext) || StringUtils.isBlank(hash)) {
            return false;
        }
        return BCrypt.checkpw(plaintext, hash);
    }
}
