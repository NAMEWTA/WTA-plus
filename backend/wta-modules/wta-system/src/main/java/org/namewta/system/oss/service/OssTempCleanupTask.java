package org.namewta.system.oss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 临时对象清理入口。首次启用前必须先审核 dry-run 结果。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssTempCleanupTask {

    private final OssLifecycleProperties properties;
    private final OssLifecycleManager manager;

    @Scheduled(cron = "${oss.lifecycle.cleanup-cron:0 30 * * * ?}")
    public void cleanup() {
        if (!properties.isCleanupEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long ossId : manager.findExpiredTempIds(now)) {
            try {
                boolean matched = manager.cleanupExpired(ossId, now, properties.isCleanupDryRun());
                if (matched) {
                    log.info("OSS 临时对象清理{}: ossId={}", properties.isCleanupDryRun() ? "待执行" : "已处理", ossId);
                }
            } catch (RuntimeException e) {
                log.error("OSS 临时对象清理失败: ossId={}", ossId, e);
            }
        }
    }
}
