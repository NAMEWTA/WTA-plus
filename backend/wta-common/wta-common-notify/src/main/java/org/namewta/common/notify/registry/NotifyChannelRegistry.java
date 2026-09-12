package org.namewta.common.notify.registry;

import org.namewta.common.notify.exception.NotifyValidationException;
import org.namewta.common.notify.model.NotifyChannel;
import org.namewta.common.notify.spi.NotifyChannelAdapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知渠道 Adapter 注册表。
 */
public final class NotifyChannelRegistry {

    private final Map<NotifyChannel, NotifyChannelAdapter> adapters;

    public NotifyChannelRegistry(List<NotifyChannelAdapter> adapters) {
        Map<NotifyChannel, NotifyChannelAdapter> registry = new LinkedHashMap<>();
        if (adapters != null) {
            for (NotifyChannelAdapter adapter : adapters) {
                if (adapter == null || adapter.channel() == null) {
                    throw new IllegalStateException("通知渠道 Adapter 必须声明 channel");
                }
                NotifyChannel channel = adapter.channel();
                if (registry.putIfAbsent(channel, adapter) != null) {
                    throw new IllegalStateException("重复的通知渠道 Adapter: " + channel);
                }
            }
        }
        this.adapters = Map.copyOf(registry);
    }

    public NotifyChannelAdapter require(NotifyChannel channel) {
        NotifyChannelAdapter adapter = adapters.get(channel);
        if (adapter == null) {
            throw new NotifyValidationException("UNKNOWN_CHANNEL", "未知通知渠道: " + channel);
        }
        return adapter;
    }
}
