package org.namewta.notify.usecase;

import lombok.RequiredArgsConstructor;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.core.exception.ServiceException;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.notify.port.ProviderCallbackPort;
import org.namewta.notify.port.SmsReceiptResultPort;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.time.Instant;

/** 回调验签、解析和幂等更新用例。 */
@Service
@RequiredArgsConstructor
public class ProviderCallbackUseCase implements SmsReceiptResultPort {
    private static final String NATIVE_SMS_PREFIX = "native-sms:";
    private final ProviderCallbackPort callbackService;

    @DSTransactional
    public void apply(String channel, String signature, String payload, String secret) {
        if (!verify(payload, signature, secret)) throw new ServiceException("回调验签失败", 401);
        Map<String, Object> body;
        try {
            body = JsonUtils.parseMap(payload);
        } catch (RuntimeException malformed) {
            throw new IllegalArgumentException("回调 JSON 格式错误");
        }
        if (body == null || !Set.of("eventId", "timestamp", "providerKey", "providerMessageId", "status", "target").containsAll(body.keySet())
            || body.values().stream().anyMatch(value -> !(value instanceof String))) {
            throw new IllegalArgumentException("回调字段不合法");
        }
        String eventId = text(body, "eventId");
        if (eventId.startsWith(NATIVE_SMS_PREFIX)) throw new IllegalArgumentException("回调事件编号使用保留前缀");
        String timestamp = text(body, "timestamp");
        String providerKey = text(body, "providerKey");
        if (eventId.isBlank() || timestamp.isBlank() || providerKey.isBlank()) {
            throw new IllegalArgumentException("回调事件元数据不完整");
        }
        try {
            Instant sentAt = Instant.parse(timestamp);
            Instant now = Instant.now();
            if (sentAt.isBefore(now.minusSeconds(300)) || sentAt.isAfter(now.plusSeconds(300))) {
                throw new IllegalArgumentException("回调已过期");
            }
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException("回调时间格式错误");
        }
        callbackService.apply(channel, providerKey, text(body, "providerMessageId"), text(body, "status"), eventId, text(body, "target"));
    }

    /** 已鉴权HTTPS查询产生内部身份，与外部自定义事件隔离；仍复用同一持久事务。 */
    @Override
    @DSTransactional
    public void confirm(String supplier, String providerKey, String messageId, String target, String status) {
        if (!Set.of("tencent", "alibaba").contains(supplier)
            || !Set.of("DELIVERED", "UNDELIVERABLE").contains(status)) {
            throw new IllegalArgumentException("短信查询结果不合法");
        }
        try {
            String identity = JsonUtils.toJsonString(java.util.List.of(supplier, messageId, target, status));
            String eventId = NATIVE_SMS_PREFIX + HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(identity.getBytes(StandardCharsets.UTF_8)));
            callbackService.apply("SMS", providerKey, messageId, status, eventId, target);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String text(Map<String, Object> body, String name) {
        return (String) body.getOrDefault(name, "");
    }

    private boolean verify(String payload, String signature, String secret) {
        if (secret == null || secret.isBlank() || signature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.security.MessageDigest.isEqual(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)), HexFormat.of().parseHex(signature));
        } catch (Exception exception) { return false; }
    }
}
