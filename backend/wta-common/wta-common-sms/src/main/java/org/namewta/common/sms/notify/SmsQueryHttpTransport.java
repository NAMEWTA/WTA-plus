package org.namewta.common.sms.notify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 每次查询拥有并关闭其连接和响应；禁止重定向及无界响应。生产端点由调用者固定。 */
final class SmsQueryHttpTransport implements SmsDeliveryQueryClient.Transport {
    static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final int TIMEOUT_MS = 5000;

    @Override
    public String post(URI endpoint, Map<String, String> headers, String body) {
        if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("SMS_QUERY_INTERRUPTED");
        HttpURLConnection connection = null;
        long started = System.nanoTime();
        try {
            connection = (HttpURLConnection) endpoint.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            headers.forEach(connection::setRequestProperty);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (var output = connection.getOutputStream()) { output.write(bytes); }
            if (connection.getResponseCode() != 200) throw new IllegalStateException("SMS_QUERY_HTTP_ERROR");
            if (connection.getContentLengthLong() > MAX_RESPONSE_BYTES) throw new IllegalStateException("SMS_QUERY_RESPONSE_TOO_LARGE");
            try (InputStream input = connection.getInputStream(); var output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("SMS_QUERY_INTERRUPTED");
                    if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started) >= 15) {
                        throw new IllegalStateException("SMS_QUERY_DEADLINE");
                    }
                    if (output.size() + count > MAX_RESPONSE_BYTES) throw new IllegalStateException("SMS_QUERY_RESPONSE_TOO_LARGE");
                    output.write(buffer, 0, count);
                }
                return output.toString(StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            // 保留内部cause用于定位；上层只记录固定错误类别，禁止打印请求URL/供应商响应。
            throw new IllegalStateException("SMS_QUERY_IO_ERROR", exception);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
