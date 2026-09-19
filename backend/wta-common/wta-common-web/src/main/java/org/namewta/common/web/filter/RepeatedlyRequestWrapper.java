package org.namewta.common.web.filter;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.namewta.common.core.constant.Constants;
import org.namewta.common.core.http.CapturedRequestBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 可重复读取原始正文；观察者共享入口缓存，不因新增包装器复制完整数组。 */
public class RepeatedlyRequestWrapper extends HttpServletRequestWrapper {
    private final CapturedRequestBody body;

    /**
     * 首个缓存入口决定预算；机器入口已验过其独立预算时，只复用已捕获原文。
     * @param request 原始或已包装请求
     * @param response 当前响应
     * @param maxBodyBytes 普通正文缓存上限
     * @throws IOException 读取失败或正文超限
     */
    public RepeatedlyRequestWrapper(HttpServletRequest request, ServletResponse response, int maxBodyBytes)
        throws IOException {
        super(request);
        CapturedRequestBody.requireLimit(maxBodyBytes);
        request.setCharacterEncoding(Constants.UTF8);
        response.setCharacterEncoding(Constants.UTF8);
        CapturedRequestBody existing = CapturedRequestBody.find(request);
        body = existing != null ? existing : CapturedRequestBody.capture(request, maxBodyBytes);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override public ServletInputStream getInputStream() { return body.openStream(); }
    @Override public int getContentLength() { return body.length(); }
    @Override public long getContentLengthLong() { return body.length(); }

    /** 返回原始正文实际字节数。 */
    public int getBodyLength() { return body.length(); }

    /** 日志只复制所需前缀，保留业务与验签字节不变。 */
    public byte[] getBodyPrefix(int maxLength) { return body.prefix(maxLength); }
}
