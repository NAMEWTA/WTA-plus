package org.namewta.common.notify.event;

/**
 * 通知监控事件发布接缝。
 */
@FunctionalInterface
public interface NotifyEventPublisher {

    void publish(NotifyDeliveryEvent event);
}
