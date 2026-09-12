package org.namewta.common.web.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.CompositeConverter;

/**
 * 为 sys-console 文件选择行格式：HTTP 专用 logger 原样输出 JSON，其他日志沿用文本 pattern。
 */
public class SysConsoleLineConverter extends CompositeConverter<ILoggingEvent> {

    @Override
    protected String transform(ILoggingEvent event, String genericLine) {
        if (SysLogEventWriter.LOGGER_NAME.equals(event.getLoggerName())) {
            return event.getFormattedMessage() + CoreConstants.LINE_SEPARATOR;
        }
        return genericLine + CoreConstants.LINE_SEPARATOR;
    }
}
