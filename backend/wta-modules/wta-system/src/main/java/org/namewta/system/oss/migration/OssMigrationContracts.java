package org.namewta.system.oss.migration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class OssMigrationContracts {

    private OssMigrationContracts() {
    }

    public record MigrationRequest(@NotEmpty List<@NotNull Long> ossIds,
                                   @NotBlank String targetConfigKey) {
        public MigrationRequest {
            ossIds = ossIds == null ? List.of() : List.copyOf(ossIds);
        }
    }

    public record CleanupRequest(boolean approved) {
    }

    public record PreflightItem(Long ossId, String sourceConfigKey, String targetConfigKey,
                                String objectKey, boolean ready, String reason) {
    }

    public record DryRunReport(String targetConfigKey, boolean ready, List<PreflightItem> items) {
        public DryRunReport {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record BatchView(Long batchId, String targetConfigKey, OssMigrationStatus status,
                            int totalCount, int successCount, int failedCount,
                            Instant startedTime, Instant completedTime) {
    }

    public record ItemView(Long itemId, Long ossId, String sourceConfigKey, String targetConfigKey,
                           String objectKey, OssMigrationStatus status, OssMigrationStage stage,
                           int retryCount, OssMigrationStage lastErrorStage, String errorMessage,
                           Instant cleanupEligibleTime) {
    }
}
