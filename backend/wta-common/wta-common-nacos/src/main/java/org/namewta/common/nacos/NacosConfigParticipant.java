package org.namewta.common.nacos;

import org.springframework.boot.context.properties.bind.Binder;

import java.util.Set;

/**
 * 即时生效配置的无副作用准备接缝。
 *
 * @param <T> 不可变或仅由调用方读取的配置快照类型
 */
public interface NacosConfigParticipant<T> {

    String id();

    Set<String> prefixes();

    /**
     * 即时生效的精确配置键。用于不能安全表达为前缀的单键合同。
     */
    default Set<String> exactKeys() {
        return Set.of();
    }

    T prepare(Binder binder);
}
