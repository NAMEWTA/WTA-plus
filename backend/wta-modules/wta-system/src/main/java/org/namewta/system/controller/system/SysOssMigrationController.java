package org.namewta.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.redis.annotation.RepeatSubmit;
import org.namewta.system.oss.migration.OssMigrationContracts.*;
import org.namewta.system.oss.migration.OssStorageMigrationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/resource/oss/migrations")
public class SysOssMigrationController {

    private final OssStorageMigrationService migrationService;

    @SaCheckPermission("system:ossMigration:list")
    @GetMapping("/{batchId}")
    public R<BatchView> batch(@NotNull @PathVariable Long batchId) {
        return R.ok(migrationService.batch(batchId));
    }

    @SaCheckPermission("system:ossMigration:list")
    @GetMapping("/{batchId}/items")
    public R<List<ItemView>> items(@NotNull @PathVariable Long batchId) {
        return R.ok(migrationService.items(batchId));
    }

    @SaCheckPermission("system:ossMigration:execute")
    @Log(title = "OSS迁移预检", businessType = BusinessType.OTHER, isSaveRequestData = false)
    @RepeatSubmit
    @PostMapping("/dry-run")
    public R<DryRunReport> dryRun(@Valid @RequestBody MigrationRequest request) {
        return R.ok(migrationService.dryRun(request));
    }

    @SaCheckPermission("system:ossMigration:execute")
    @Log(title = "OSS迁移启动", businessType = BusinessType.OTHER, isSaveRequestData = false)
    @RepeatSubmit
    @PostMapping("/start")
    public R<Long> start(@Valid @RequestBody MigrationRequest request) {
        return R.ok(migrationService.start(request));
    }

    @SaCheckPermission("system:ossMigration:execute")
    @Log(title = "OSS迁移重试", businessType = BusinessType.OTHER, isSaveRequestData = false)
    @RepeatSubmit
    @PostMapping("/{batchId}/retry")
    public R<Void> retry(@NotNull @PathVariable Long batchId) {
        migrationService.retry(batchId);
        return R.ok();
    }

    @SaCheckPermission("system:ossMigration:rollback")
    @Log(title = "OSS迁移回滚", businessType = BusinessType.UPDATE, isSaveRequestData = false)
    @RepeatSubmit
    @PostMapping("/{batchId}/rollback")
    public R<Void> rollback(@NotNull @PathVariable Long batchId) {
        migrationService.rollback(batchId);
        return R.ok();
    }

    @SaCheckPermission("system:ossMigration:cleanup")
    @Log(title = "OSS迁移源清理", businessType = BusinessType.CLEAN, isSaveRequestData = false)
    @RepeatSubmit
    @PostMapping("/{batchId}/cleanup")
    public R<Void> cleanup(@NotNull @PathVariable Long batchId,
                           @Valid @RequestBody CleanupRequest request) {
        migrationService.cleanup(batchId, request.approved());
        return R.ok();
    }
}
