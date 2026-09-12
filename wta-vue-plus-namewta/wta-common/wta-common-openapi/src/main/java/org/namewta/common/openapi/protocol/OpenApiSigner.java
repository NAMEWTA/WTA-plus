package org.namewta.common.openapi.protocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * HMAC-SHA256 signing and verification for NAMEWTA v1.
 */
public final class OpenApiSigner {

    private final OpenApiCanonicalizer canonicalizer;

    public OpenApiSigner(OpenApiCanonicalizer canonicalizer) {
        this.canonicalizer = canonicalizer;
    }

    public String sign(OpenApiRequest request, String appSecret) {
        byte[] key = decodeSecret(appSecret);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] signature = mac.doFinal(canonicalizer.canonicalize(request).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is not available", exception);
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
        }
    }

    public boolean verify(OpenApiRequest request, String appSecret, String suppliedSignature) {
        try {
            byte[] expected = Base64.getUrlDecoder().decode(sign(request, appSecret));
            byte[] actual = Base64.getUrlDecoder().decode(suppliedSignature);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] decodeSecret(String appSecret) {
        try {
            byte[] key = Base64.getUrlDecoder().decode(appSecret);
            if (key.length < 32) {
                throw new OpenApiAuthenticationException();
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new OpenApiAuthenticationException(exception);
        }
    }

}
