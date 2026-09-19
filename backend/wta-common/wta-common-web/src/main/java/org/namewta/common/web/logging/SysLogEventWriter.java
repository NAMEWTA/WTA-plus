package org.namewta.common.web.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 使用项目 JsonMapper 向专用 INFO logger 输出中文字段的完整单行事件。
 */
public class SysLogEventWriter implements SysLogEventSink {

    /**
     * Logback 用于识别无前缀 HTTP JSON 行的稳定 logger 名。
     */
    public static final String LOGGER_NAME = "org.namewta.system.http";

    private static final Logger HTTP_LOG = LoggerFactory.getLogger(LOGGER_NAME);
    private static final Map<String, String> FIELD_NAMES = Map.ofEntries(
        Map.entry("timestamp", "记录时间"),
        Map.entry("event", "事件类型"),
        Map.entry("requestId", "请求标识"),
        Map.entry("method", "请求方法"),
        Map.entry("path", "请求路径"),
        Map.entry("parameters", "请求参数"),
        Map.entry("requestHeaders", "请求头"),
        Map.entry("responseHeaders", "响应头"),
        Map.entry("contentType", "内容类型"),
        Map.entry("contentLength", "内容长度"),
        Map.entry("upstreamRequestId", "上游请求标识"),
        Map.entry("status", "响应状态"),
        Map.entry("durationMs", "耗时毫秒"),
        Map.entry("completed", "处理完成"),
        Map.entry("bodyLogged", "正文已记录"),
        Map.entry("bodyLength", "正文长度"),
        Map.entry("truncated", "正文已截断"),
        Map.entry("body", "正文"),
        Map.entry("bodyOmissionReason", "正文省略原因")
    );
    private static final Map<String, String> EVENT_NAMES = Map.of(
        "HTTP_REQUEST", "请求进入",
        "HTTP_RESPONSE", "响应返回"
    );
    private static final Map<String, String> BODY_OMISSION_REASONS = Map.ofEntries(
        Map.entry("NO_BODY", "无正文"),
        Map.entry("CREDENTIAL_RESPONSE", "凭据签发仅记录元数据"),
        Map.entry("CONTENT_TYPE_MISSING", "未提供内容类型"),
        Map.entry("INVALID_CONTENT_TYPE", "内容类型无效"),
        Map.entry("MULTIPART", "多部分表单"),
        Map.entry("FORM_PARAMETERS_ONLY", "仅记录表单参数"),
        Map.entry("NON_TEXT_CONTENT_TYPE", "非文本内容"),
        Map.entry("ATTACHMENT", "附件内容"),
        Map.entry("STREAMING", "流式内容"),
        Map.entry("BODY_CAPTURE_UNAVAILABLE", "正文采集不可用")
    );

    private final JsonMapper jsonMapper;

    public SysLogEventWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void write(Map<String, Object> event) {
        Map<String, Object> localizedEvent = new LinkedHashMap<>();
        event.forEach((name, value) -> localizedEvent.put(
            FIELD_NAMES.getOrDefault(name, name), localizedValue(name, value)));
        HTTP_LOG.info(jsonMapper.writeValueAsString(localizedEvent));
    }

    private Object localizedValue(String name, Object value) {
        if ("event".equals(name) && value instanceof String eventName) {
            return EVENT_NAMES.getOrDefault(eventName, eventName);
        }
        if ("bodyOmissionReason".equals(name) && value instanceof String omissionReason) {
            return BODY_OMISSION_REASONS.getOrDefault(omissionReason, omissionReason);
        }
        return value;
    }
}
