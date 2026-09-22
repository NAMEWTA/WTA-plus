package org.namewta.system.oss.upload;

import org.namewta.common.oss.constant.OssConstant;
import org.namewta.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

/**
 * 从 Redis 读取 status=Y 的 OSS 配置键。
 */
@Component
public class RedisOssDefaultStorageKey implements OssDefaultStorageKey {

    @Override
    public String current() {
        return RedisUtils.getCacheObject(OssConstant.DEFAULT_CONFIG_KEY);
    }
}
