package org.namewta.system.listener;

import com.baomidou.dynamic.datasource.annotation.DsTxEventListener;
import lombok.RequiredArgsConstructor;
import org.namewta.system.auth.ExternalAuthConfigurationCache;
import org.namewta.system.event.ExternalAuthConfigChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;

/** 沿用 OSS 的动态数据源提交后缓存刷新机制。 */
@Component
@RequiredArgsConstructor
public class ExternalAuthConfigChangeListener {
    private final ExternalAuthConfigurationCache cache;

    /** DsTxEventListener 默认 AFTER_COMMIT；回滚不发出共享失效。 */
    @DsTxEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(ExternalAuthConfigChangedEvent event) {
        cache.invalidate(event.keys());
    }
}
