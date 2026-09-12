package org.namewta.notify.port;

import java.time.Duration;

/**
 * 发送配额计数端口。
 */
public interface NotifyQuotaPort {

    /**
     * 尝试占用一个限额名额。超限时不得留下计数。
     *
     * @param key    计数键
     * @param limit  窗口内上限，小于等于 0 表示不限制
     * @param window 窗口
     * @return 未超限为 {@code true}
     */
    boolean tryAcquire(String key, int limit, Duration window);

    /**
     * 释放一次已成功占用的限额。未占用过的键应保持幂等。
     *
     * @param key 计数键
     */
    default void release(String key) {
        // 无计数的桩实现无需回滚
    }
}
