package org.namewta.system.event;

import java.util.List;

/** 在业务事务提交后清除共享缓存的旧版本键，不携带配置或密钥。 */
public record ExternalAuthConfigChangedEvent(List<String> keys) {
    public ExternalAuthConfigChangedEvent {
        keys = List.copyOf(keys);
    }
}
