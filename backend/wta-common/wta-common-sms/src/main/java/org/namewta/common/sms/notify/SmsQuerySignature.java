package org.namewta.common.sms.notify;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** 原生只读短信查询使用的 TC3 / ACS3 请求签名，不参与 SMS4J 发送。 */
final class SmsQuerySignature {
    private SmsQuerySignature() { }

    static String tencent(String keyId, String secret, String host, String body, Instant now) {
        String date = now.atOffset(ZoneOffset.UTC).toLocalDate().toString();
        String scope = date + "/sms/tc3_request";
        String headers = "content-type;host";
        String canonical = "POST\n/\n\ncontent-type:application/json; charset=utf-8\nhost:" + host
            + "\n\n" + headers + "\n" + sha256(body);
        String toSign = "TC3-HMAC-SHA256\n" + now.getEpochSecond() + "\n" + scope + "\n" + sha256(canonical);
        byte[] signing = hmac(hmac(hmac(bytes("TC3" + secret), date), "sms"), "tc3_request");
        return "TC3-HMAC-SHA256 Credential=" + keyId + "/" + scope + ", SignedHeaders=" + headers
            + ", Signature=" + HexFormat.of().formatHex(hmac(signing, toSign));
    }

    static String alibaba(String keyId, String secret, String query, Map<String, String> headers) {
        var sorted = new TreeMap<>(headers);
        String names = String.join(";", sorted.keySet());
        String canonicalHeaders = sorted.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue().trim() + "\n")
            .collect(Collectors.joining());
        String canonical = "POST\n/\n" + query + "\n" + canonicalHeaders + "\n" + names + "\n" + sha256("");
        return "ACS3-HMAC-SHA256 Credential=" + keyId + ",SignedHeaders=" + names + ",Signature="
            + HexFormat.of().formatHex(hmac(bytes(secret), "ACS3-HMAC-SHA256\n" + sha256(canonical)));
    }

    static String query(Map<String, String> values) {
        return new TreeMap<>(values).entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
            .collect(Collectors.joining("&"));
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes(value)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SMS_QUERY_CRYPTO_UNAVAILABLE", exception);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    private static byte[] hmac(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(bytes(value));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SMS_QUERY_CRYPTO_UNAVAILABLE", exception);
        }
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
