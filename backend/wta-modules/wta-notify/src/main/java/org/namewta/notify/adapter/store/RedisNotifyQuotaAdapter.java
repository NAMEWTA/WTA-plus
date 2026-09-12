package org.namewta.notify.adapter.store;

import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.notify.port.NotifyQuotaPort;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 使用 Redis 原子计数实现发送配额。
 */
@Component
public class RedisNotifyQuotaAdapter implements NotifyQuotaPort {

    /**
     * 占用限额。
     *
     * @param key    计数键
     * @param limit  上限
     * @param window 窗口
     * @return 是否允许发送
     */
    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        if (limit <= 0) {
            return true;
        }
        long value = RedisUtils.incrAtomicValue(key);
        if (value == 1) {
            RedisUtils.expire(key, window);
        }
        if (value <= limit) {
            return true;
        }
        RedisUtils.decrAtomicValue(key);
        return false;
    }

    /**
     * 回滚一次成功占用。
     *
     * @param key 计数键
     */
    @Override
    public void release(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        RedisUtils.decrAtomicValue(key);
    }
}
