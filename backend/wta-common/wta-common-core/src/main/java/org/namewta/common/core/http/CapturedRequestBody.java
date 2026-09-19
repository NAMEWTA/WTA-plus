package org.namewta.common.core.http;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.namewta.common.core.exception.RequestBodyTooLargeException;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** 每次请求唯一的有界原始正文所有者；只提供独立读取游标或防御性副本。 */
public final class CapturedRequestBody {
    public static final int DEFAULT_MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_CONFIGURED_BYTES = Integer.MAX_VALUE - 8;
    private static final String ATTRIBUTE = CapturedRequestBody.class.getName();
    private final byte[] bytes;
    private final int limit;

    private CapturedRequestBody(byte[] ownedBytes, int limit) {
        this.bytes = ownedBytes;
        this.limit = limit;
    }

    /** 返回已由入口捕获的原始正文；包装器的属性委托不会复制字节。 */
    public static CapturedRequestBody find(HttpServletRequest request) {
        return request.getAttribute(ATTRIBUTE) instanceof CapturedRequestBody body ? body : null;
    }

    /**
     * 校验声明长度并至多读取预算加一字节；流由Servlet容器拥有，异常原样向上游传播。
     * 已有缓存仍检查调用方预算；下游观察者应直接复用find结果，不另设认证入口预算。
     */
    public static CapturedRequestBody capture(HttpServletRequest request, int maxBytes) throws IOException {
        requireLimit(maxBytes);
        CapturedRequestBody previous = find(request);
        if (previous != null) {
            if (previous.length() > maxBytes) throw new RequestBodyTooLargeException();
            return previous;
        }
        if (request.getContentLengthLong() > maxBytes) throw new RequestBodyTooLargeException();
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(8192, maxBytes));
        byte[] buffer = new byte[Math.min(8192, maxBytes + 1)];
        InputStream input = request.getInputStream();
        int total = 0;
        while (true) {
            int count = input.read(buffer, 0, Math.min(buffer.length, maxBytes - total + 1));
            if (count < 0) break;
            if (count == 0) {
                int value = input.read();
                if (value < 0) break;
                if (total == maxBytes) throw new RequestBodyTooLargeException();
                output.write(value);
                total++;
            } else {
                if (count > maxBytes - total) throw new RequestBodyTooLargeException();
                output.write(buffer, 0, count);
                total += count;
            }
        }
        CapturedRequestBody body = new CapturedRequestBody(output.toByteArray(), maxBytes);
        request.setAttribute(ATTRIBUTE, body);
        return body;
    }

    /** 视图拥有自己的有界UTF-8字节，永不替换请求属性中的原始验签正文。 */
    public CapturedRequestBody utf8View(String value) throws RequestBodyTooLargeException {
        if (value.length() > limit) throw new RequestBodyTooLargeException();
        byte[] view = value.getBytes(StandardCharsets.UTF_8);
        if (view.length > limit) throw new RequestBodyTooLargeException();
        return new CapturedRequestBody(view, limit);
    }

    /** 拒绝零、负值或超出JVM数组边界的配置，不提供无界模式。 */
    public static void requireLimit(int maxBytes) {
        if (maxBytes <= 0 || maxBytes > MAX_CONFIGURED_BYTES) {
            throw new IllegalArgumentException("请求正文字节上限必须为正数且在JVM数组范围内");
        }
    }

    /** JSON及结构化+json使用相同字节预算，不以字符串前缀误判其他媒体类型。 */
    public static boolean isJsonContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) return false;
        try {
            MediaType type = MediaType.parseMediaType(contentType);
            return "json".equalsIgnoreCase(type.getSubtype())
                || type.getSubtype().toLowerCase(java.util.Locale.ROOT).endsWith("+json");
        } catch (InvalidMediaTypeException exception) {
            return false;
        }
    }

    public int length() { return bytes.length; }
    public String utf8() { return new String(bytes, StandardCharsets.UTF_8); }

    /** 供不可变签名合同使用的防御性副本；日志应只取有界prefix。 */
    public byte[] copy() { return Arrays.copyOf(bytes, bytes.length); }

    public byte[] prefix(int maxLength) {
        if (maxLength < 0) throw new IllegalArgumentException("前缀长度不能为负");
        return Arrays.copyOf(bytes, Math.min(bytes.length, maxLength));
    }

    /** 独立游标不复制底层数组；仅供同步MVC消费，异步上传不进入此缓存。 */
    public ServletInputStream openStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        return new ServletInputStream() {
            @Override public int read() { return input.read(); }
            @Override public int read(byte[] target, int offset, int length) { return input.read(target, offset, length); }
            @Override public int available() { return input.available(); }
            @Override public boolean isFinished() { return input.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("缓存正文使用同步读取");
            }
        };
    }
}
