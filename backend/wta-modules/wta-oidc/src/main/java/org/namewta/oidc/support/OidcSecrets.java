package org.namewta.oidc.support;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** 不透明凭据和摘要的纯安全函数。 */
public final class OidcSecrets {
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 工具类不允许实例化，调用方使用静态协议操作。 */
    private OidcSecrets() {}

    /** 使用安全随机源产生不可预测的不透明凭据。 */
    public static String random() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** 计算凭据摘要用于索引，避免数据库索引保存明文。 */
    public static String hash(String raw) {
        if (raw == null) return null;
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 以恒定时间字节比较校验非空凭据。 */
    public static boolean equal(String a, String b) {
        return a != null
                && b != null
                && MessageDigest.isEqual(
                        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
