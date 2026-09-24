package org.namewta.common.json.utils;

import org.namewta.common.core.constant.SystemConstants;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * HTTP 与操作日志共用的副本策略。只输出结构化脱敏数据；失败或无字段语义的正文不回退原文。
 * 不可用于转换业务响应、签名输入或持久化业务对象。
 */
public final class LogSanitizer {

    public static final String REDACTED = "[REDACTED]";

    private static final Set<String> SENSITIVE_NAMES = Set.of(
        "passwordhash", "temporarypassword", "token", "tokenid", "uploadtoken", "accesstoken", "refreshtoken", "idtoken",
        "secret", "appsecret", "signature", "canonicalrequest", "machinetoken", "internaltoken",
        "clientsecret", "secretkey", "accesskey", "apikey", "privatekey", "signingkey",
        "authorization", "cookie", "sessionid", "credential", "credentials", "captcha", "captchacode",
        "smscode", "emailcode", "verifycode", "verificationcode",
        // 自由文本错误可能回显输入，无法仅靠字段名证明安全。
        "msg", "message", "errormsg", "errormessage", "error"
    );
    private static final Set<String> OAUTH_NAMES = Set.of("code", "codeverifier", "verifier", "redirecturi", "redirecturl", "state");
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "authorization", "proxyauthorization", "cookie", "setcookie", "xapikey", "apikey", "xappkey",
        "xsignature", "xnotifysignature", "xauthtoken", "xcsrftoken", "encryptkey", "referer", "location"
    );
    private static final Set<String> CREDENTIAL_RESPONSES = Set.of(
        "/auth/login", "/auth/social/callback", "/sso/login", "/sso/oauth2/token", "/sso/oauth2/authorize"
    );

    private LogSanitizer() {
    }

    /** 判断规范化字段或表单嵌套字段是否含凭据；OAuth code 仅在认证路径屏蔽。 */
    public static boolean isSensitiveName(String name, String requestPath, String... excludedNames) {
        if (name == null) {
            return false;
        }
        if (isNotifyCallbackPath(requestPath)) return true;
        String normalized = normalize(name);
        if (SENSITIVE_NAMES.contains(normalized)
            || isCredentialQueryName(normalized)
            || (isSmsCaptchaPath(requestPath) && ("phonenumber".equals(normalized) || "phone".equals(normalized)))
            || (isNotifySubmissionPath(requestPath) && Set.of("bizid", "recipientids", "templateparams",
                "idempotencykey", "metadata").contains(normalized))
            || Arrays.stream(SystemConstants.EXCLUDE_PROPERTIES).anyMatch(value -> normalize(value).equals(normalized))
            || Arrays.stream(excludedNames).anyMatch(value -> value != null && normalize(value).equals(normalized))
            || (isOAuthPath(requestPath) && OAUTH_NAMES.contains(normalized))) {
            return true;
        }
        // Servlet 表单仍可能以 user[password]、items[0].token 表达嵌套数据。
        return Arrays.stream(name.split("[.\\[\\]]+")).filter(part -> !part.equals(name))
            .anyMatch(part -> isSensitiveName(part, requestPath, excludedNames));
    }

    /** URL 类头可能携带签名或授权码，所有日志入口只记录其存在。 */
    public static boolean isSensitiveHeader(String name) {
        return name != null && (SENSITIVE_HEADERS.contains(normalize(name)) || isSensitiveName(name, null));
    }

    /** 凭据签发与授权跳转的 HTTP/操作日志均仅保留元数据。 */
    public static boolean omitResponseBody(String requestPath) {
        return CREDENTIAL_RESPONSES.contains(normalizePath(requestPath));
    }

    /** 完整 JSON 脱敏后再由调用者截断；非法、标量正文或序列化失败只返回固定摘要。 */
    public static String json(String raw, String requestPath, String... excludedNames) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        if (isNotifyCallbackPath(requestPath)) return REDACTED;
        try {
            JsonNode node = JsonUtils.getJsonMapper().reader()
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(raw);
            if (node == null || (!node.isObject() && !node.isArray())) {
                return REDACTED;
            }
            redact(node, requestPath, excludedNames);
            return JsonUtils.toJsonString(node);
        } catch (RuntimeException failure) {
            return REDACTED;
        }
    }

    /** 转换独立树副本，禁止修改传入的 Map、DTO 或 JsonNode。 */
    public static String object(Object value, String requestPath, String... excludedNames) {
        if (value == null) {
            return null;
        }
        if (isNotifyCallbackPath(requestPath)) return REDACTED;
        if (value instanceof CharSequence text) {
            return json(text.toString(), requestPath, excludedNames);
        }
        try {
            JsonNode node = JsonUtils.getJsonMapper().valueToTree(value).deepCopy();
            if (node.isTextual()) {
                return REDACTED;
            }
            redact(node, requestPath, excludedNames);
            return JsonUtils.toJsonString(node);
        } catch (RuntimeException failure) {
            return REDACTED;
        }
    }

    /** 异常消息及 cause 可含原始输入；日志只记录类型，原异常仍由业务链抛出。 */
    public static String failure(Throwable failure) {
        return failure == null ? null : failure.getClass().getName();
    }

    /** 仅转换日志路径副本；在线设备的两个 token 路由隐藏整段令牌，业务路由仍使用原路径。 */
    public static String path(String applicationPath) {
        if (applicationPath == null) {
            return null;
        }
        String uploadPrefix = "/resource/oss/uploads/";
        if (applicationPath.startsWith(uploadPrefix)) {
            String remaining = applicationPath.substring(uploadPrefix.length());
            int slash = remaining.indexOf('/');
            String token = slash < 0 ? remaining : remaining.substring(0, slash);
            String suffix = slash < 0 ? "" : remaining.substring(slash);
            if (!token.isEmpty()) {
                return uploadPrefix + REDACTED + suffix;
            }
        }
        String prefix = "/monitor/online/";
        if (!applicationPath.startsWith(prefix)) {
            return applicationPath;
        }
        String remaining = applicationPath.substring(prefix.length());
        String selfPrefix = "myself/";
        if (remaining.startsWith(selfPrefix)) {
            String tokenSegment = remaining.substring(selfPrefix.length());
            return isSingleTokenSegment(tokenSegment)
                ? prefix + selfPrefix + REDACTED + trailingSlash(tokenSegment) : applicationPath;
        }
        if ("list".equals(remaining) || "list/".equals(remaining) || "myself".equals(remaining)) {
            return applicationPath;
        }
        return isSingleTokenSegment(remaining)
            ? prefix + REDACTED + trailingSlash(remaining) : applicationPath;
    }

    private static boolean isSingleTokenSegment(String segment) {
        if (segment.isEmpty()) {
            return false;
        }
        int slash = segment.indexOf('/');
        return slash == -1 || (slash == segment.length() - 1 && segment.length() > 1);
    }

    private static String trailingSlash(String segment) {
        return segment.endsWith("/") ? "/" : "";
    }

    private static void redact(JsonNode node, String requestPath, String[] excludedNames) {
        if (node instanceof ObjectNode object) {
            for (Map.Entry<String, JsonNode> property : object.properties()) {
                if (isSensitiveName(property.getKey(), requestPath, excludedNames)) {
                    object.put(property.getKey(), REDACTED);
                } else if (property.getValue().isTextual() && hasCredentialUrl(property.getValue().asText())) {
                    object.put(property.getKey(), REDACTED);
                } else {
                    redact(property.getValue(), requestPath, excludedNames);
                }
            }
        } else if (node instanceof ArrayNode array) {
            for (int index = 0; index < array.size(); index++) {
                JsonNode child = array.get(index);
                if (child.isTextual() && hasCredentialUrl(child.asText())) {
                    array.set(index, REDACTED);
                } else {
                    redact(child, requestPath, excludedNames);
                }
            }
        }
    }

    /** 只识别 URL 查询键，不把普通业务 code/state 当作凭据。非法 URI 仍按原文本检查。 */
    private static boolean hasCredentialUrl(String value) {
        if (!(value.regionMatches(true, 0, "https://", 0, 8)
            || value.regionMatches(true, 0, "http://", 0, 7))) {
            return false;
        }
        int question = value.indexOf('?');
        if (question < 0) {
            return false;
        }
        int fragment = value.indexOf('#', question + 1);
        String query = value.substring(question + 1, fragment < 0 ? value.length() : fragment);
        for (String parameter : query.split("[&;]")) {
            String rawName = parameter.split("=", 2)[0];
            String decodedName;
            try {
                decodedName = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException malformed) {
                // 非法百分号编码不能绕过明显的明文凭据键。
                decodedName = rawName;
            }
            if (isCredentialQueryName(normalize(decodedName))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCredentialQueryName(String normalized) {
        return normalized.contains("credential") || normalized.contains("signature")
            || normalized.contains("accesskey") || normalized.contains("secret")
            || normalized.endsWith("token") || "apikey".equals(normalized);
    }

    private static boolean isOAuthPath(String requestPath) {
        String path = normalizePath(requestPath);
        return path.startsWith("/sso/") || path.startsWith("/auth/social/") || path.equals("/auth/login");
    }

    /** 回执包含精确收件地址，HTTP 与操作日志均只保留元数据；不改变验签原文。 */
    private static boolean isNotifyCallbackPath(String requestPath) {
        String path = normalizePath(requestPath);
        return path.equals("/notify/callback") || path.startsWith("/notify/callback/");
    }

    private static boolean isSmsCaptchaPath(String requestPath) {
        return "/resource/sms/code".equals(normalizePath(requestPath));
    }

    private static boolean isNotifySubmissionPath(String requestPath) {
        return "/notify/notification".equals(normalizePath(requestPath));
    }

    private static String normalizePath(String requestPath) {
        if (requestPath == null) {
            return "";
        }
        return requestPath.replaceAll(";[^/]*", "").replaceAll("/+$", "");
    }

    private static String normalize(String value) {
        return value.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
    }
}
