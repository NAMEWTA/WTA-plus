package org.namewta.system.oss.migration;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "oss.migration")
public class OssStorageMigrationProperties implements InitializingBean {

    static final long MIN_VERIFY_BYTES = 1024L * 1024;
    static final long MAX_VERIFY_BYTES = 5L * 1024 * 1024 * 1024;

    private int maxBatchSize = 100;
    private long maxVerifyBytes = 64L * 1024 * 1024;
    private Duration cleanupDelay = Duration.ofHours(24);
    private Duration ioTimeout = Duration.ofSeconds(5);

    @Override
    public void afterPropertiesSet() {
        if (maxBatchSize < 1 || maxBatchSize > 1000) {
            throw new IllegalStateException("OSS migration maxBatchSize 超出安全范围");
        }
        if (maxVerifyBytes < MIN_VERIFY_BYTES || maxVerifyBytes > MAX_VERIFY_BYTES) {
            throw new IllegalStateException("OSS migration maxVerifyBytes 超出安全范围");
        }
        if (cleanupDelay == null || cleanupDelay.compareTo(Duration.ofMinutes(1)) < 0
            || cleanupDelay.compareTo(Duration.ofDays(30)) > 0) {
            throw new IllegalStateException("OSS migration cleanupDelay 超出安全范围");
        }
        if (ioTimeout == null || ioTimeout.compareTo(Duration.ofMillis(100)) < 0
            || ioTimeout.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalStateException("OSS migration ioTimeout 超出安全范围");
        }
    }
}
