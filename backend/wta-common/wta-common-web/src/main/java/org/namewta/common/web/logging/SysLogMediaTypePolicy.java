package org.namewta.common.web.logging;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import java.util.Locale;

/**
 * 系统日志正文分类策略。文件、二进制和持续流只允许记录元数据。
 */
public final class SysLogMediaTypePolicy {

    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    private SysLogMediaTypePolicy() {
    }

    /**
     * 判断请求正文是否属于可重复读取并记录的普通文本。
     *
     * @param contentType 请求 Content-Type
     * @return 是否记录正文
     */
    public static boolean isRequestBodyLoggable(String contentType) {
        BodyDecision decision = requestDecision(contentType);
        return decision.loggable();
    }

    static BodyDecision requestDecision(String contentType) {
        MediaType mediaType = parse(contentType);
        if (mediaType == null) {
            return BodyDecision.omit(contentType == null ? "CONTENT_TYPE_MISSING" : "INVALID_CONTENT_TYPE");
        }
        if ("multipart".equalsIgnoreCase(mediaType.getType())) {
            return BodyDecision.omit("MULTIPART");
        }
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
            return BodyDecision.omit("FORM_PARAMETERS_ONLY");
        }
        if (MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mediaType)) {
            return BodyDecision.omit("STREAMING");
        }
        return isTextual(mediaType) ? BodyDecision.allow() : BodyDecision.omit("NON_TEXT_CONTENT_TYPE");
    }

    static BodyDecision responseDecision(HttpServletResponse response) {
        String disposition = response.getHeader(CONTENT_DISPOSITION);
        if (disposition != null && disposition.toLowerCase(Locale.ROOT).contains("attachment")) {
            return BodyDecision.omit("ATTACHMENT");
        }
        MediaType mediaType = parse(response.getContentType());
        if (mediaType == null) {
            return BodyDecision.omit(response.getContentType() == null
                ? "CONTENT_TYPE_MISSING" : "INVALID_CONTENT_TYPE");
        }
        if (MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mediaType)) {
            return BodyDecision.omit("STREAMING");
        }
        return isTextual(mediaType) ? BodyDecision.allow() : BodyDecision.omit("NON_TEXT_CONTENT_TYPE");
    }

    static boolean isJson(String contentType) {
        MediaType mediaType = parse(contentType);
        if (mediaType == null) {
            return false;
        }
        String subtype = mediaType.getSubtype().toLowerCase(Locale.ROOT);
        return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType) || subtype.endsWith("+json");
    }

    private static MediaType parse(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException ignored) {
            return null;
        }
    }

    private static boolean isTextual(MediaType mediaType) {
        String subtype = mediaType.getSubtype().toLowerCase(Locale.ROOT);
        return "text".equalsIgnoreCase(mediaType.getType())
            || MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)
            || subtype.endsWith("+json")
            || MediaType.APPLICATION_XML.isCompatibleWith(mediaType)
            || subtype.endsWith("+xml")
            || "javascript".equals(subtype)
            || "graphql".equals(subtype);
    }

    record BodyDecision(boolean loggable, String omissionReason) {

        static BodyDecision allow() {
            return new BodyDecision(true, null);
        }

        static BodyDecision omit(String reason) {
            return new BodyDecision(false, reason);
        }
    }
}
