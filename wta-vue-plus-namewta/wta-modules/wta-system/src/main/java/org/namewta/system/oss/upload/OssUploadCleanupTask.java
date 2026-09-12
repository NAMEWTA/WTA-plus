package org.namewta.system.oss.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 未完成上传主动清理。生产启用前需先审核 dry-run 输出和 Bucket Lifecycle。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssUploadCleanupTask {

    private final OssUploadProperties properties;
    private final OssUploadTicketStore ticketStore;
    private final OssUploadService uploadService;

    @Scheduled(fixedDelayString = "${oss.direct-upload.cleanup-delay:PT10M}")
    public void cleanup() {
        if (!properties.isCleanupEnabled()) {
            return;
        }
        for (String token : ticketStore.findExpired(System.currentTimeMillis(), properties.getCleanupBatchSize())) {
            try {
                if (uploadService.cleanupExpired(token, properties.isCleanupDryRun())) {
                    log.info("OSS 未完成上传清理命中，tokenSuffix={}, dryRun={}", suffix(token),
                        properties.isCleanupDryRun());
                }
            } catch (RuntimeException e) {
                log.warn("OSS 未完成上传清理失败，tokenSuffix={}, error={}", suffix(token),
                    e.getClass().getSimpleName());
            }
        }
    }

    private String suffix(String token) {
        return token == null || token.length() < 8 ? "invalid" : token.substring(token.length() - 8);
    }
}
