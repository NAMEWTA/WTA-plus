package org.namewta.sso.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 与OIDC公开sid一致的SHA256 URL安全摘要，不存储原始Cookie。 */
public final class SsoSessionHashes {
    private SsoSessionHashes() {}

    /** 计算稳定摘要，仅用于已经校验格式的随机会话标识。 */
    public static String hash(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
