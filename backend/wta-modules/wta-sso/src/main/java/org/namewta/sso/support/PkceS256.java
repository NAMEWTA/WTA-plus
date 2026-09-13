package org.namewta.sso.support;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;

/**
 * PKCE S256：challenge = BASE64URL(SHA256(verifier))。
 */
public final class PkceS256 {

    public static final String METHOD = "S256";

    private PkceS256() {
    }

    /**
     * 计算 S256 挑战。
     *
     * @param verifier 明文 verifier
     * @return BASE64URL 挑战
     */
    public static String challenge(String verifier) {
        if (StringUtils.isBlank(verifier)) {
            throw new ServiceException("缺少 code_verifier");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /**
     * 校验 method 必须为 S256。
     *
     * @param method 挑战方法
     */
    public static void requireS256(String method) {
        if (StringUtils.isBlank(method) || !METHOD.equals(method.trim().toUpperCase(Locale.ROOT))) {
            throw new ServiceException("code_challenge_method 必须为 S256");
        }
    }

    /**
     * 校验 challenge 存在。
     *
     * @param challenge 挑战
     */
    public static void requireChallenge(String challenge) {
        if (StringUtils.isBlank(challenge)) {
            throw new ServiceException("缺少 code_challenge");
        }
    }

    /**
     * 用 verifier 校验绑定的挑战。
     *
     * @param verifier  提交的 verifier
     * @param challenge 签发时绑定的挑战
     */
    public static void verify(String verifier, String challenge) {
        if (StringUtils.isBlank(verifier) || StringUtils.isBlank(challenge)) {
            throw new ServiceException("PKCE 校验失败");
        }
        if (verifier.length() < 43 || verifier.length() > 128) {
            throw new ServiceException("PKCE 校验失败");
        }
        if (!challenge.equals(challenge(verifier))) {
            throw new ServiceException("PKCE 校验失败");
        }
    }
}
