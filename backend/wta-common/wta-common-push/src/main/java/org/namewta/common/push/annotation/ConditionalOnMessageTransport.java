package org.namewta.common.push.annotation;

import org.namewta.common.push.condition.MessageTransportCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/**
 * 按消息推送传输方式启用组件。
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(MessageTransportCondition.class)
public @interface ConditionalOnMessageTransport {

    /**
     * 传输方式：sse / websocket。
     */
    String value();

}
