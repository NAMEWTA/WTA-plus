package org.namewta.sso.support;

import java.security.SecureRandom;
import java.util.Base64;

/** SSO授权码与会话标识的256位随机值；数据库主键不使用此入口。 */
public final class SsoBearerTokens {
    private static final SecureRandom RANDOM = new SecureRandom();

    private SsoBearerTokens() { }

    /** @return 无填充、URL-safe的32字节随机值 */
    public static String create() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * @param value 外来会话标识
     * @return 是否符合当前会话标识的长度和编码；不接受旧雪花ID标识
     */
    public static boolean isValid(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{43}");
    }
}
