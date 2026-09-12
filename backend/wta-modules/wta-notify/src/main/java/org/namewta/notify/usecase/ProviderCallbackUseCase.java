package org.namewta.notify.usecase;

import lombok.RequiredArgsConstructor;
import org.namewta.common.json.utils.JsonUtils;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.notify.port.ProviderCallbackPort;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.time.Instant;
import java.time.Duration;

/** 回调验签、解析和幂等更新用例。 */
@Service
@RequiredArgsConstructor
public class ProviderCallbackUseCase {
    private final ProviderCallbackPort callbackService;

    @DSTransactional
    public void apply(String channel, String signature, String payload, String secret) {
        if (!verify(payload, signature, secret)) throw new IllegalArgumentException("回调验签失败");
        Map<String, Object> body = JsonUtils.parseMap(payload);
        String eventId = Objects.toString(body.get("eventId"), "");
        String timestamp = Objects.toString(body.get("timestamp"), "");
        String providerKey = Objects.toString(body.get("providerKey"), "");
        if (eventId.isBlank() || timestamp.isBlank() || providerKey.isBlank()) {
            throw new IllegalArgumentException("鍥炶皟浜嬩欢鍏冩暟鎹笉瀹屾暣");
        }
        try {
            if (Math.abs(Duration.between(Instant.parse(timestamp), Instant.now()).toSeconds()) > 300) {
                throw new IllegalArgumentException("鍥炶皟宸茶繃鏈");
            }
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException("鍥炶皟鏃堕棿鏍煎紡閿欒");
        }
        String providerMessageId = Objects.toString(body.get("providerMessageId"), "");
        String status = Objects.toString(body.get("status"), "");
        if (providerMessageId.isBlank() || status.isBlank()) throw new IllegalArgumentException("回调参数不完整");
        callbackService.apply(channel, providerKey, providerMessageId, status, eventId);
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

