package org.namewta.common.web.logging;

import java.nio.charset.StandardCharsets;

/**
 * 一个方向的正文日志结果。
 */
record SysLogBody(boolean logged, long length, boolean truncated, String body, String omissionReason) {

    static SysLogBody logged(byte[] prefix, long fullLength, int maxBytes) {
        int availableLength = Math.min(prefix.length, maxBytes);
        int safeLength = fullLength > availableLength
            ? safeUtf8Length(prefix, availableLength)
            : availableLength;
        return new SysLogBody(
            true,
            fullLength,
            fullLength > safeLength,
            new String(prefix, 0, safeLength, StandardCharsets.UTF_8),
            null);
    }

    static SysLogBody omitted(long length, String reason) {
        return new SysLogBody(false, length, false, null, reason);
    }

    private static int safeUtf8Length(byte[] bytes, int limit) {
        if (limit == 0) {
            return limit;
        }
        int lead = limit - 1;
        while (lead >= 0 && (bytes[lead] & 0xC0) == 0x80) {
            lead--;
        }
        if (lead < 0) {
            return 0;
        }
        int first = bytes[lead] & 0xFF;
        int sequenceLength;
        if ((first & 0x80) == 0) {
            sequenceLength = 1;
        } else if ((first & 0xE0) == 0xC0) {
            sequenceLength = 2;
        } else if ((first & 0xF0) == 0xE0) {
            sequenceLength = 3;
        } else if ((first & 0xF8) == 0xF0) {
            sequenceLength = 4;
        } else {
            return lead;
        }
        return lead + sequenceLength <= limit ? limit : lead;
    }
}
