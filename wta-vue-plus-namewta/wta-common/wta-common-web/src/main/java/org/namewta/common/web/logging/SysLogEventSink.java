package org.namewta.common.web.logging;

import java.util.Map;

/**
 * HTTP 结构化事件输出接缝。
 */
@FunctionalInterface
public interface SysLogEventSink {

    /**
     * 输出一个完整 HTTP JSON 事件。
     *
     * @param event 事件字段
     */
    void write(Map<String, Object> event);
}
