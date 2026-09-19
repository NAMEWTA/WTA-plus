package org.namewta.common.openapi.gateway;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.namewta.common.core.http.CapturedRequestBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** 机器入口有界捕获原始字节；验签与MVC使用同一正文，后续观察者复用只读缓存。 */
final class ReplayableOpenApiRequest extends HttpServletRequestWrapper {
    private final CapturedRequestBody body;

    ReplayableOpenApiRequest(HttpServletRequest request, int maxBodyBytes) throws IOException {
        super(request);
        body = CapturedRequestBody.capture(request, maxBodyBytes);
    }

    byte[] body() { return body.copy(); }
    @Override public int getContentLength() { return body.length(); }
    @Override public long getContentLengthLong() { return body.length(); }

    @Override
    public BufferedReader getReader() {
        String encoding = getCharacterEncoding();
        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }

    @Override public ServletInputStream getInputStream() { return body.openStream(); }
}
