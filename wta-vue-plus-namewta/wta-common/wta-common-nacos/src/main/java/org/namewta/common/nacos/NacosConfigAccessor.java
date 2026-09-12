package org.namewta.common.nacos;

import java.util.Optional;

/**
 * 读取已原子准备的运行期配置快照。
 */
public interface NacosConfigAccessor {

    <T> Optional<T> configuration(String participantId, Class<T> type);
}
