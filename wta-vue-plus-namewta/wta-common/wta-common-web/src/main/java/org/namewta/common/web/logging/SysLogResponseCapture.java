package org.namewta.common.web.logging;

import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;

/**
 * 响应直通写出时的有界日志副本；无论是否记录正文，都统计实际观察到的写出字节。
 */
final class SysLogResponseCapture {

    private final int maxBodyBytes;
    private final ByteArrayOutputStream prefix;
    private long bodyLength;
    private boolean missedBytes;

    SysLogResponseCapture(int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
        this.prefix = new ByteArrayOutputStream(Math.min(maxBodyBytes, 8192));
    }

    synchronized void capture(int value, HttpServletResponse response) {
        bodyLength++;
        if (SysLogMediaTypePolicy.responseDecision(response).loggable()) {
            if (!missedBytes && prefix.size() < maxBodyBytes) {
                prefix.write(value);
            }
        } else {
            missedBytes = true;
        }
    }

    synchronized void capture(byte[] bytes, int offset, int length, HttpServletResponse response) {
        bodyLength += length;
        if (SysLogMediaTypePolicy.responseDecision(response).loggable()) {
            if (!missedBytes && prefix.size() < maxBodyBytes) {
                int copyLength = Math.min(length, maxBodyBytes - prefix.size());
                prefix.write(bytes, offset, copyLength);
            }
        } else {
            missedBytes = true;
        }
    }

    synchronized void reset() {
        prefix.reset();
        bodyLength = 0;
        missedBytes = false;
    }

    synchronized SysLogBody snapshot(HttpServletResponse response) {
        if (bodyLength == 0) {
            return SysLogBody.omitted(0, "NO_BODY");
        }
        SysLogMediaTypePolicy.BodyDecision decision = SysLogMediaTypePolicy.responseDecision(response);
        if (!decision.loggable()) {
            return SysLogBody.omitted(bodyLength, decision.omissionReason());
        }
        if (missedBytes) {
            return SysLogBody.omitted(bodyLength, "BODY_CAPTURE_UNAVAILABLE");
        }
        return SysLogBody.logged(prefix.toByteArray(), bodyLength, maxBodyBytes);
    }
}
