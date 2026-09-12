package org.namewta.third.api;

import tools.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Map;

/**
 * 第三方调用请求合同。调用方只能提交 provider/endpoint 编码和元数据声明的值。
 */
public record ThirdPartyRequest(
    String providerCode,
    String endpointCode,
    Map<String, ?> path,
    Map<String, ?> query,
    Map<String, String> headers,
    JsonNode body
) {
    public ThirdPartyRequest {
        if (providerCode == null || providerCode.isBlank()) {
            throw new IllegalArgumentException("providerCode is required");
        }
        if (endpointCode == null || endpointCode.isBlank()) {
            throw new IllegalArgumentException("endpointCode is required");
        }
        path = path == null ? Collections.emptyMap() : Map.copyOf(path);
        query = query == null ? Collections.emptyMap() : Map.copyOf(query);
        headers = headers == null ? Collections.emptyMap() : Map.copyOf(headers);
    }

    public static ThirdPartyRequest of(String providerCode, String endpointCode) {
        return new ThirdPartyRequest(providerCode, endpointCode, Map.of(), Map.of(), Map.of(), null);
    }
}
