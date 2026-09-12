package org.namewta.common.oss.model;

import java.time.Instant;
import java.util.Map;

/**
 * 浏览器可直接执行的 OSS 预签名请求。
 *
 * @param method          HTTP 方法
 * @param url             短时签名 URL
 * @param requiredHeaders 调用方必须原样发送的请求头
 * @param expiresAt       绝对过期时间
 */
public record OssPresignedRequest(
    String method,
    String url,
    Map<String, String> requiredHeaders,
    Instant expiresAt
) {

    public OssPresignedRequest {
        requiredHeaders = requiredHeaders == null ? Map.of() : Map.copyOf(requiredHeaders);
    }
}
