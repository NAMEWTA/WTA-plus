package org.namewta.common.web.logging;

import org.namewta.common.json.utils.LogSanitizer;

/** 在 HTTP 日志副本中应用共享策略；不完整或非 JSON 正文只输出固定摘要。 */
final class SysLogBodySanitizer {

    private SysLogBodySanitizer() {
    }

    static SysLogBody sanitize(SysLogBody body, String contentType, String requestPath) {
        if (!body.logged()) {
            return body;
        }
        // 不完整前缀不能证明脱敏完成，绝不输出截断前的原始片段。
        String safe = !body.truncated() && SysLogMediaTypePolicy.isJson(contentType)
            ? LogSanitizer.json(body.body(), requestPath) : LogSanitizer.REDACTED;
        return new SysLogBody(true, body.length(), body.truncated(), safe, null);
    }
}
