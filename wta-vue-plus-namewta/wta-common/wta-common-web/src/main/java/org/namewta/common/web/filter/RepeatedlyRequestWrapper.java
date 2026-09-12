package org.namewta.common.web.filter;

import cn.hutool.core.io.IoUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.namewta.common.core.constant.Constants;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 构建可重复读取输入流的请求包装器，缓存请求体以支持多次读取。
 *
 * @author wta
 */
public class RepeatedlyRequestWrapper extends HttpServletRequestWrapper {
    /**
     * 请求体字节数据。
     */
    private final byte[] body;

    /**
     * 读取原始请求体并缓存到内存，统一设置请求与响应编码。
     *
     * @param request  原始请求
     * @param response 当前响应
     * @throws IOException 读取请求体异常
     */
    public RepeatedlyRequestWrapper(HttpServletRequest request, ServletResponse response) throws IOException {
        super(request);
        request.setCharacterEncoding(Constants.UTF8);
        response.setCharacterEncoding(Constants.UTF8);

        body = IoUtil.readBytes(request.getInputStream(), false);
    }

    /**
     * 基于缓存的请求体构造字符读取器。
     *
     * @return 可重复读取的字符流
     * @throws IOException IO 异常
     */
    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * 返回基于缓存请求体重新生成的输入流。
     *
     * @return 可重复读取的输入流
     * @throws IOException IO 异常
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            /**
             * 读取缓存请求体的下一个字节。
             *
             * @return 下一个字节
             * @throws IOException IO 异常
             */
            @Override
            public int read() throws IOException {
                return bais.read();
            }

            /**
             * 返回缓存请求体剩余可读字节数。
             *
             * @return 剩余字节数
             * @throws IOException IO 异常
             */
            @Override
            public int available() throws IOException {
                return bais.available();
            }

            /**
             * 判断缓存请求体是否已读取完毕。
             *
             * @return 是否读取完毕
             */
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            /**
             * 判断输入流是否可读。
             *
             * @return 固定为 true
             */
            @Override
            public boolean isReady() {
                return true;
            }

            /**
             * 设置异步读取监听器。
             *
             * @param readListener 读取监听器
             */
            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }

    /**
     * 返回请求体的实际 UTF-8 字节数。
     *
     * @return 请求体字节数
     */
    public int getBodyLength() {
        return body.length;
    }

    /**
     * 复制不超过指定上限的请求体前缀，避免日志截断再复制完整大正文。
     *
     * @param maxLength 最大字节数
     * @return 请求体前缀
     */
    public byte[] getBodyPrefix(int maxLength) {
        return Arrays.copyOf(body, Math.min(body.length, maxLength));
    }
}
