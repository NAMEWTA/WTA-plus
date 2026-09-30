package org.namewta.common.web.advice;

import org.namewta.common.json.enhance.JsonValueEnhancer;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体统一增强拦截器。
 */
@RestControllerAdvice
public class ResponseEnhancementAdvice implements ResponseBodyAdvice<Object> {

    private final JsonValueEnhancer jsonValueEnhancer;
    private final java.util.List<org.namewta.common.core.service.HttpProtocolPolicy> protocolPolicies;
    /** 保留既有无协议适配器的构造入口。 */
    public ResponseEnhancementAdvice(JsonValueEnhancer enhancer) {
        this(enhancer, java.util.List.of());
    }
    /** 为标准协议保留响应原文，其他业务继续使用统一增强。 */
    public ResponseEnhancementAdvice(JsonValueEnhancer enhancer,
            java.util.List<org.namewta.common.core.service.HttpProtocolPolicy> policies) {
        this.jsonValueEnhancer = enhancer;
        this.protocolPolicies = java.util.List.copyOf(policies);
    }


    @Override
    public boolean supports(@NonNull MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return jsonValueEnhancer.supports(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {
        if (!selectedContentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return body;
        }
        String path = request.getURI().getPath();
        if (protocolPolicies.stream().anyMatch(policy -> policy.isProtocolPath(path))) return body;
        return jsonValueEnhancer.enhance(body);
    }

}
