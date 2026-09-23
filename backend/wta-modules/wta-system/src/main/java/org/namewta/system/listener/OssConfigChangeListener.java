package org.namewta.system.listener;

import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.oss.constant.OssConstant;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.common.redis.utils.CacheUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.system.event.OssConfigChangeEvent;
import org.namewta.system.oss.readiness.OssStorageReadinessService;
import org.springframework.stereotype.Component;

/**
 * OSS 配置变更监听器。
 */
@Component
@RequiredArgsConstructor
public class OssConfigChangeListener {

    private final OssStorageReadinessService readinessService;

    /**
     * 数据提交后同步刷新 OSS 配置缓存与客户端实例。
     *
     * @param event OSS 配置变更事件
     */
    @DsTxEventListener
    public void refreshOssConfig(OssConfigChangeEvent event) {
        try {
            if (event.defaultConfig()) {
                RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, event.configKey());
                return;
            }
            if (StringUtils.isNotBlank(event.oldConfigKey())
                && !StringUtils.equals(event.oldConfigKey(), event.configKey())) {
                CacheUtils.evict(CacheNames.SYS_OSS_CONFIG, event.oldConfigKey());
                OssFactory.remove(event.oldConfigKey());
            }
            if (StringUtils.isBlank(event.configKey())) {
                return;
            }
            if (StringUtils.isBlank(event.configJson())) {
                CacheUtils.evict(CacheNames.SYS_OSS_CONFIG, event.configKey());
            } else {
                CacheUtils.put(CacheNames.SYS_OSS_CONFIG, event.configKey(), event.configJson());
            }
            OssFactory.remove(event.configKey());
        } finally {
            readinessService.refresh();
        }
    }

}
