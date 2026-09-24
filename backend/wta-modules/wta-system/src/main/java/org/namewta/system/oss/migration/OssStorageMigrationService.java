package org.namewta.system.oss.migration;

import org.namewta.system.domain.SysOss;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.namewta.system.oss.migration.OssMigrationContracts.*;

@Service
public class OssStorageMigrationService {

    private final OssMigrationStore store;
    private final OssMigrationObjectStore objectStore;
    private final OssMigrationAccessVerifier accessVerifier;
    private final OssStorageReadinessRegistry readinessRegistry;
    private final OssStorageMigrationProperties properties;
    private final Clock clock;
    private final OssMigrationAtomicService atomic;

    @Autowired
    public OssStorageMigrationService(OssMigrationStore store, OssMigrationObjectStore objectStore,
                                      OssMigrationAccessVerifier accessVerifier,
                                      OssStorageReadinessRegistry readinessRegistry,
                                      OssStorageMigrationProperties properties, OssMigrationAtomicService atomic) {
        this(store, objectStore, accessVerifier, readinessRegistry, properties, Clock.systemUTC(), atomic);
    }

    public OssStorageMigrationService(OssMigrationStore store, OssMigrationObjectStore objectStore,
                                      OssMigrationAccessVerifier accessVerifier,
                                      OssStorageReadinessRegistry readinessRegistry,
                                      OssStorageMigrationProperties properties, Clock clock,
                                      OssMigrationAtomicService atomic) {
        this.store = store;
        this.objectStore = objectStore;
        this.accessVerifier = accessVerifier;
        this.readinessRegistry = readinessRegistry;
        this.properties = properties;
        this.clock = clock;
        this.atomic = atomic;
    }

    public long publish(Long ossId, String targetConfigKey) {
        if (ossId == null || ossId <= 0) {
            throw new OssMigrationException(OssMigrationError.INVALID_REQUEST, "公开对象无效");
        }
        return start(new MigrationRequest(List.of(ossId), targetConfigKey));
    }

    public void unpublish(Long ossId) {
        SysOssMigrationItem item = ossId == null ? null : store.findLatestRestorable(ossId);
        if (item == null) {
            throw new OssMigrationException(OssMigrationError.INVALID_STATE, "没有可恢复的公开工单");
        }
        if (!objectStore.exists(item.getSourceConfigKey(), item.getObjectKey(), properties.getIoTimeout())) {
            throw new OssMigrationException(OssMigrationError.OBJECT_NOT_FOUND, "来源对象已不存在，不能恢复");
        }
        atomic.restore(item.getOssMigrationItemId(), item.getVersion());
        refreshBatch(requireBatch(item.getOssMigrationBatchId()));
    }

    public DryRunReport dryRun(MigrationRequest request) {
        List<Long> ids = validateRequest(request);
        Map<Long, SysOss> objects = new LinkedHashMap<>();
        store.findObjects(ids).forEach(oss -> objects.put(oss.getOssId(), oss));
        List<PreflightItem> results = new ArrayList<>(ids.size());
        for (Long ossId : ids) {
            SysOss oss = objects.get(ossId);
            if (oss == null) {
                results.add(new PreflightItem(ossId, null, request.targetConfigKey(), null,
                    false, OssMigrationError.OBJECT_NOT_FOUND.name()));
                continue;
            }
            String source = oss.getService();
            try {
                if (!"ACTIVE".equals(oss.getDeleteState())) {
                    throw new OssMigrationException(OssMigrationError.INVALID_STATE,
                        "仅允许迁移正常状态的 OSS 对象");
                }
                if (source == null || source.isBlank()) {
                    throw new OssMigrationException(OssMigrationError.STORAGE_NOT_SERVING,
                        "来源存储配置不存在");
                }
                if (source.equals(request.targetConfigKey())) {
                    throw new OssMigrationException(OssMigrationError.INVALID_REQUEST,
                        "来源与目标存储不能相同");
                }
                OssMigrationObjectStore.Inspection inspection = objectStore.inspect(source,
                    request.targetConfigKey(), oss.getFileName(), properties.getMaxVerifyBytes());
                if (!inspection.sourceExists()) {
                    throw new OssMigrationException(OssMigrationError.OBJECT_NOT_FOUND, "来源对象不存在");
                }
                if (inspection.conflict()) {
                    throw new OssMigrationException(OssMigrationError.TARGET_CONFLICT, "目标对象内容冲突");
                }
                results.add(new PreflightItem(ossId, source, request.targetConfigKey(), oss.getFileName(),
                    true, null));
            } catch (OssMigrationException ex) {
                results.add(new PreflightItem(ossId, source, request.targetConfigKey(), oss.getFileName(),
                    false, ex.error().name()));
            } catch (RuntimeException ex) {
                results.add(new PreflightItem(ossId, source, request.targetConfigKey(), oss.getFileName(),
                    false, OssMigrationError.COPY_FAILED.name()));
            }
        }
        return new DryRunReport(request.targetConfigKey(), results.stream().allMatch(PreflightItem::ready), results);
    }

    public long start(MigrationRequest request) {
        List<Long> ids = validateRequest(request);
        Map<Long, OssMigrationStore.ConfigIdentity> sources = new HashMap<>();
        for (SysOss object : store.findObjects(ids)) {
            sources.put(object.getOssId(), store.configIdentity(object.getService()));
        }
        OssMigrationStore.ConfigIdentity target = store.configIdentity(request.targetConfigKey());
        DryRunReport report = dryRun(request);
        if (!report.ready()) {
            throw new OssMigrationException(OssMigrationError.INVALID_REQUEST, "迁移预检未通过");
        }
        Map<Long, SysOss> objects = new LinkedHashMap<>();
        store.findObjects(report.items().stream().map(PreflightItem::ossId).toList())
            .forEach(oss -> objects.put(oss.getOssId(), oss));
        SysOssMigrationBatch batch = store.createBatch(request.targetConfigKey(), report.items().size());
        for (PreflightItem result : report.items()) {
            SysOssMigrationItem item = atomic.createItem(batch.getOssMigrationBatchId(), result.ossId(),
                sources.get(result.ossId()), target);
            process(item);
        }
        refreshBatch(batch);
        return batch.getOssMigrationBatchId();
    }

    public BatchView batch(Long batchId) {
        return view(requireBatch(batchId));
    }

    public List<ItemView> items(Long batchId) {
        requireBatch(batchId);
        return store.listItems(batchId).stream().map(this::view).toList();
    }

    public void retry(Long batchId) {
        SysOssMigrationBatch batch = requireBatch(batchId);
        for (SysOssMigrationItem item : store.listItems(batchId)) {
            if (item.getStatus() == OssMigrationStatus.FAILED
                && item.getLastErrorStage() != OssMigrationStage.COMPLETED) {
                process(item);
            }
        }
        refreshBatch(batch);
    }

    public void rollback(Long batchId) {
        SysOssMigrationBatch batch = requireBatch(batchId);
        for (SysOssMigrationItem item : store.listItems(batchId)) {
            if (item.getStatus().terminal()) {
                continue;
            }
            if (OssMigrationAtomicService.unresolved(item)) {
                throw new OssMigrationException(OssMigrationError.INVALID_STATE, "来源删除结果未确认，不能恢复");
            }
            if (!objectStore.exists(item.getSourceConfigKey(), item.getObjectKey(), properties.getIoTimeout())) {
                throw new OssMigrationException(OssMigrationError.OBJECT_NOT_FOUND, "来源对象已不存在，不能恢复");
            }
            atomic.restore(item.getOssMigrationItemId(), item.getVersion());
        }
        refreshBatch(batch);
    }

    public void cleanup(Long batchId, boolean approved) {
        if (!approved) {
            throw new OssMigrationException(OssMigrationError.CLEANUP_NOT_APPROVED, "源对象清理尚未批准");
        }
        SysOssMigrationBatch batch = requireBatch(batchId);
        List<SysOssMigrationItem> items = store.listItems(batchId);
        if (items.stream().anyMatch(item -> item.getStatus() != OssMigrationStatus.CLEANUP_ELIGIBLE
            && item.getStatus() != OssMigrationStatus.COMPLETED
            && !OssMigrationAtomicService.unresolved(item))) {
            throw new OssMigrationException(OssMigrationError.INVALID_STATE, "批次尚不可清理");
        }
        try {
            for (SysOssMigrationItem item : items) {
                if (item.getStatus() == OssMigrationStatus.COMPLETED) {
                    continue;
                }
                OssMigrationAtomicService.Reservation reservation;
                if (OssMigrationAtomicService.unresolved(item)) {
                    reservation = OssMigrationAtomicService.reservation(item);
                    reconcile(reservation);
                } else {
                    // 只有数据库已确认持久化执行权之后，才允许进入不可撤销的远端 DELETE。
                    reservation = atomic.reserveCleanup(item.getOssMigrationItemId(), clock.instant());
                    try {
                        objectStore.delete(reservation.source(), reservation.key(), properties.getIoTimeout());
                    } catch (RuntimeException ex) {
                        throw new OssMigrationException(OssMigrationError.CLEANUP_FAILED,
                            "来源清理结果未知，请核对来源后重试", ex);
                    }
                    reconcile(reservation);
                }
            }
        } catch (RuntimeException failure) {
            try {
                refreshBatch(batch);
            } catch (RuntimeException refreshFailure) {
                if (refreshFailure != failure) failure.addSuppressed(refreshFailure);
            }
            throw failure;
        }
        refreshBatch(batch);
    }

    private void reconcile(OssMigrationAtomicService.Reservation reservation) {
        try {
            if (objectStore.exists(reservation.source(), reservation.key(), properties.getIoTimeout())) {
                throw new OssMigrationException(OssMigrationError.CLEANUP_FAILED,
                    "来源对象仍存在，不能重发删除");
            }
            if (!objectStore.exists(reservation.target(), reservation.key(), properties.getIoTimeout())) {
                throw new OssMigrationException(OssMigrationError.CLEANUP_FAILED,
                    "目标对象不可确认，不能完成清理");
            }
        } catch (OssMigrationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new OssMigrationException(OssMigrationError.CLEANUP_FAILED,
                "来源或目标状态未知，请人工核对", ex);
        }
        atomic.finalizeCleanup(reservation);
    }

    private void process(SysOssMigrationItem item) {
        item = atomic.claim(item.getOssMigrationItemId(), item.getVersion());
        item = atomic.copied(item.getOssMigrationItemId(), item.getVersion());
        OssMigrationObjectStore.Transfer transfer;
        try {
            transfer = objectStore.transferAndVerify(item.getSourceConfigKey(),
                item.getTargetConfigKey(), item.getObjectKey(), properties.getMaxVerifyBytes());
        } catch (OssMigrationException ex) {
            OssMigrationStage stage = ex.error() == OssMigrationError.CONTENT_MISMATCH
                ? OssMigrationStage.CONTENT_VERIFIED : OssMigrationStage.COPIED;
            atomic.fail(item.getOssMigrationItemId(), item.getVersion(), stage, ex.error(), false);
            return;
        } catch (RuntimeException ex) {
            atomic.fail(item.getOssMigrationItemId(), item.getVersion(), OssMigrationStage.COPIED,
                OssMigrationError.COPY_FAILED, false);
            return;
        }
        item = atomic.verified(item.getOssMigrationItemId(), item.getVersion(), transfer);
        try {
            item = atomic.switchToTarget(item.getOssMigrationItemId(), item.getVersion());
        } catch (OssMigrationException ex) {
            if (ex.error() != OssMigrationError.SERVICE_DRIFT
                && ex.error() != OssMigrationError.OBJECT_NOT_FOUND) throw ex;
            atomic.fail(item.getOssMigrationItemId(), item.getVersion(), OssMigrationStage.SERVICE_SWITCHED,
                ex.error(), false);
            return;
        }
        try {
            accessVerifier.verifyPublic(item.getOssId());
        } catch (RuntimeException ex) {
            atomic.fail(item.getOssMigrationItemId(), item.getVersion(), OssMigrationStage.ACCESS_VERIFIED,
                OssMigrationError.ACCESS_VERIFICATION_FAILED, true);
            return;
        }
        atomic.accessVerified(item.getOssMigrationItemId(), item.getVersion(),
            clock.instant().plus(properties.getCleanupDelay()));
    }

    private void refreshBatch(SysOssMigrationBatch batch) {
        List<SysOssMigrationItem> items = store.listItems(batch.getOssMigrationBatchId());
        int success = (int) items.stream().filter(item -> item.getStatus() == OssMigrationStatus.CLEANUP_ELIGIBLE
            || item.getStatus() == OssMigrationStatus.COMPLETED).count();
        int failed = (int) items.stream().filter(item -> item.getStatus() == OssMigrationStatus.FAILED).count();
        batch.setSuccessCount(success);
        batch.setFailedCount(failed);
        batch.setStatus(failed > 0 ? OssMigrationStatus.FAILED
            : !items.isEmpty() && items.stream().allMatch(item -> item.getStatus() == OssMigrationStatus.ROLLED_BACK)
            ? OssMigrationStatus.ROLLED_BACK
            : items.stream().allMatch(item -> item.getStatus().terminal())
            ? OssMigrationStatus.COMPLETED : OssMigrationStatus.CLEANUP_ELIGIBLE);
        if (items.stream().allMatch(item -> item.getStatus().terminal())) {
            batch.setCompletedTime(clock.instant());
        }
        store.saveBatch(batch);
    }

    private List<Long> validateRequest(MigrationRequest request) {
        if (request == null || request.ossIds() == null || request.targetConfigKey() == null
            || request.targetConfigKey().isBlank()) {
            throw new OssMigrationException(OssMigrationError.INVALID_REQUEST, "迁移请求无效");
        }
        List<Long> ids = request.ossIds().stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (ids.isEmpty() || ids.size() != request.ossIds().size() || ids.size() > properties.getMaxBatchSize()
            || ids.stream().anyMatch(id -> id <= 0)) {
            throw new OssMigrationException(OssMigrationError.INVALID_REQUEST, "迁移对象清单无效");
        }
        return ids;
    }

    private SysOssMigrationBatch requireBatch(Long batchId) {
        SysOssMigrationBatch batch = batchId == null ? null : store.getBatch(batchId);
        if (batch == null) {
            throw new OssMigrationException(OssMigrationError.BATCH_NOT_FOUND, "迁移批次不存在");
        }
        return batch;
    }

    private BatchView view(SysOssMigrationBatch batch) {
        return new BatchView(batch.getOssMigrationBatchId(), batch.getTargetConfigKey(), batch.getStatus(),
            batch.getTotalCount(), batch.getSuccessCount(), batch.getFailedCount(), batch.getStartedTime(),
            batch.getCompletedTime());
    }

    private ItemView view(SysOssMigrationItem item) {
        return new ItemView(item.getOssMigrationItemId(), item.getOssId(), item.getSourceConfigKey(),
            item.getTargetConfigKey(), item.getObjectKey(), item.getStatus(), item.getStage(), item.getRetryCount(),
            item.getLastErrorStage(), item.getErrorMessage(), item.getCleanupEligibleTime());
    }

}
