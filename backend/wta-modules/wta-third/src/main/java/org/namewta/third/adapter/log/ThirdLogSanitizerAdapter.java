package org.namewta.third.adapter.log;

import org.namewta.common.json.utils.JsonUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ThirdLogSanitizerAdapter {
    private static final int MAX_BYTES = 16 * 1024;
    private static final Set<String> BLOCKED = Set.of("authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key", "api-key", "apikey", "appkey", "access-key", "key", "secret", "private-key", "signature", "token", "password", "encryption", "passphrase", "credential");

    private ThirdLogSanitizerAdapter() {
    }

    public static Map<String, String> headers(Map<String, String> input) {
        java.util.LinkedHashMap<String, String> result = new java.util.LinkedHashMap<>();
        input.forEach((key, value) -> result.put(key, blocked(key) ? "***" : truncate(value)));
        return result;
    }

    public static Map<String, Object> values(Map<String, ?> input) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        input.forEach((key, value) -> result.put(key, blocked(key) ? "***" : text(value)));
        return result;
    }

    public static String json(JsonNode input) {
        return json(input, Set.of());
    }

    public static String json(JsonNode input, Set<String> additionalBlocked) {
        if (input == null) return null;
        JsonNode copy = input.deepCopy();
        redact(copy, additionalBlocked == null ? Set.of() : additionalBlocked);
        return truncate(JsonUtils.toJsonString(copy));
    }

    public static String text(Object input) {
        return input == null ? null : truncate(String.valueOf(input));
    }

    public static String value(Object input) {
        return value(input, Set.of());
    }

    public static String value(Object input, Set<String> additionalBlocked) {
        if (input == null) return null;
        if (input instanceof JsonNode node) return json(node, additionalBlocked);
        if (input instanceof byte[] bytes) {
            try {
                JsonNode node = JsonUtils.getJsonMapper().readTree(bytes);
                if (node != null) return json(node, additionalBlocked);
            } catch (RuntimeException ignored) {
                // Non-JSON response bodies are still bounded below.
            }
            return truncate(new String(bytes, StandardCharsets.UTF_8));
        }
        return text(input);
    }

    /** Returns a valid, bounded JSON scalar for persistence columns declared as JSON. */
    public static String jsonValue(Object input, Set<String> additionalBlocked) {
        String sanitized = value(input, additionalBlocked);
        if (sanitized == null) return null;
        String encoded = JsonUtils.toJsonString(sanitized);
        while (encoded.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES && sanitized.length() > 0) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
            encoded = JsonUtils.toJsonString(sanitized);
        }
        return encoded;
    }

    private static void redact(JsonNode node, Set<String> additionalBlocked) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            for (String name : new HashSet<>(object.propertyNames())) {
                if (blocked(name, additionalBlocked)) object.put(name, "***"); else redact(object.get(name), additionalBlocked);
            }
    } else if (node.isArray()) node.forEach(value -> redact(value, additionalBlocked));
    }

    private static boolean blocked(String key) {
        return blocked(key, Set.of());
    }

    private static boolean blocked(String key, Set<String> additionalBlocked) {
        String lower = key.toLowerCase(Locale.ROOT);
        return BLOCKED.stream().anyMatch(lower::contains)
            || additionalBlocked.stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(lower::equals);
    }

    private static String truncate(String value) {
        if (value == null) return null;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= MAX_BYTES) return value;
        byte[] suffix = "...[truncated]".getBytes(StandardCharsets.UTF_8);
        int contentBytes = Math.max(0, MAX_BYTES - suffix.length);
        String prefix = new String(bytes, 0, contentBytes, StandardCharsets.UTF_8);
        while (prefix.getBytes(StandardCharsets.UTF_8).length > contentBytes) prefix = prefix.substring(0, prefix.length() - 1);
        return prefix + "...[truncated]";
    }
}
