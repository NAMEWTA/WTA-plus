package org.namewta.system.oss.upload;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.namewta.system.oss.readiness.OssReadinessClientProvider;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 只报告外部 Bucket 前置条件，不读取 Secret、不修改 Bucket policy。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssUploadDiagnostics {

    private final OssStorageReadinessRegistry readinessRegistry;
    private final OssDefaultStorageKey defaultStorageKey;
    private final OssReadinessClientProvider clientProvider;

    public List<String> requirements() {
        return List.of(
            "Bucket CORS 必须只允许明确的前端 Origin 和 PUT 方法",
            "Bucket CORS 必须向浏览器暴露 ETag 响应头",
            "Bucket Lifecycle 应配置 AbortIncompleteMultipartUpload 作为应用清理兜底"
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void report() {
        readinessRegistry.snapshot().forEach((configKey, entry) -> {
            if (entry.status() == org.namewta.system.oss.readiness.OssStorageReadinessEntry.Status.SERVING) {
                log.info("OSS存储配置 [{}] readiness 通过", configKey);
            } else {
                log.warn("OSS存储配置 [{}] readiness 未通过: {}", configKey, entry.reason());
            }
        });
        String storageConfigKey = defaultStorageKey.current();
        if (storageConfigKey == null || storageConfigKey.isBlank()) {
            log.warn("OSS Direct Upload 未配置默认存储");
            return;
        }
        reportUploadPrerequisites(storageConfigKey);
    }

    private void reportUploadPrerequisites(String configKey) {
        try {
            var result = clientProvider.client(configKey).bucketConfiguration();
            if (result.compliant()) {
                log.info("OSS Direct Upload配置 [{}] CORS/Lifecycle 辅助检查通过", configKey);
            } else {
                log.warn("OSS Direct Upload配置 [{}] CORS/Lifecycle 辅助检查未通过", configKey);
            }
        } catch (RuntimeException ex) {
            log.warn("OSS Direct Upload配置 [{}] CORS/Lifecycle 辅助检查不可用", configKey);
        }
    }
}
