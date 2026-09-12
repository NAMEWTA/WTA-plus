package org.namewta.workflow.handler;

import org.dromara.warm.flow.core.dto.ApiResult;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.NodeJson;
import org.dromara.warm.flow.ui.controller.WarmFlowController;
import org.namewta.workflow.common.ConditionalOnEnable;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 规范化 Warm-Flow 设计器响应，兼容历史节点缺少签署比例的流程定义。
 *
 * <p>Warm-Flow 1.8.x 的 Vue 设计器会直接调用 {@code nodeRatio.toString()}，
 * 而旧数据中的该字段允许为 {@code null}，会导致设计器 iframe 加载后直接中断渲染。</p>
 */
@ConditionalOnEnable
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = WarmFlowController.class)
public class WarmFlowDefinitionResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethod() != null && "queryDef".equals(returnType.getMethod().getName());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof ApiResult<?> result) || !(result.getData() instanceof DefJson definition)) {
            return body;
        }
        if (definition.getNodeList() == null) {
            return body;
        }
        for (NodeJson node : definition.getNodeList()) {
            if (node != null && node.getNodeRatio() == null) {
                node.setNodeRatio("0");
            }
        }
        return body;
    }
}
