package org.namewta.common.web.logging;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 在日志副本中递归隐藏 JSON 凭据字段，不修改业务请求或响应。
 */
final class SysLogBodySanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
        "password",
        "passwordhash",
        "oldpassword",
        "newpassword",
        "confirmpassword",
        "temporarypassword",
        "token",
        "accesstoken",
        "refreshtoken",
        "idtoken",
        "secret",
        "appsecret",
        "signature",
        "canonicalrequest",
        "machinetoken",
        "internaltoken",
        "clientsecret",
        "secretkey",
        "accesskey",
        "apikey",
        "privatekey",
        "signingkey",
        "authorization",
        "cookie",
        "sessionid",
        "credential",
        "credentials",
        "captcha",
        "captchacode",
        "smscode",
        "emailcode",
        "verifycode",
        "verificationcode"
    );

    private SysLogBodySanitizer() {
    }

    static SysLogBody sanitize(SysLogBody body, String contentType) {
        if (!body.logged() || !SysLogMediaTypePolicy.isJson(contentType)) {
            return body;
        }
        try {
            JsonNode root = JSON_MAPPER.readTree(body.body());
            if (root == null) {
                return redactWholeBody(body);
            }
            redactFields(root);
            return new SysLogBody(true, body.length(), body.truncated(),
                JSON_MAPPER.writeValueAsString(root), null);
        } catch (RuntimeException exception) {
            return redactWholeBody(body);
        }
    }

    private static void redactFields(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
                if (isSensitiveName(property.getKey())) {
                    objectNode.put(property.getKey(), REDACTED);
                } else {
                    redactFields(property.getValue());
                }
            }
            return;
        }
        for (JsonNode child : node) {
            redactFields(child);
        }
    }

    static boolean isSensitiveName(String name) {
        String normalized = name.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_NAMES.contains(normalized);
    }

    private static SysLogBody redactWholeBody(SysLogBody body) {
        return new SysLogBody(true, body.length(), body.truncated(), REDACTED, null);
    }
}
