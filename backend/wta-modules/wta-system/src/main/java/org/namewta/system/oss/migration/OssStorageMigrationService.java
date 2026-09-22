package org.namewta.system.oss.migration;

import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.system.domain.SysOss;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
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

    @Autowired
    public OssStorageMigrationService(OssMigrationStore store, OssMigrationObjectStore objectStore,
                                      OssMigrationAccessVerifier accessVerifier,
                                      OssStorageReadinessRegistry readinessRegistry,
                                      OssStorageMigrationProperties properties) {
        this(store, objectStore, accessVerifier, readinessRegistry, properties, Clock.systemUTC());
    }

    public OssStorageMigrationService(OssMigrationStore store, OssMigrationObjectStore objectStore,
                                      OssMigrationAccessVerifier accessVerifier,
                                      OssStorageReadinessRegistry readinessRegistry,
                                      OssStorageMigrationProperties properties, Clock clock) {
        this.store = store;
        this.objectStore = objectStore;
        this.accessVerifier = accessVerifier;
        this.readinessRegistry = readinessRegistry;
        this.properties = properties;
        this.clock = clock;
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
        if (!objectStore.exists(item.getSourceConfigKey(), item.getObjectKey())) {
            throw new OssMigrationException(OssMigrationError.OBJECT_NOT_FOUND, "来源对象已不存在，不能恢复");
        }
        SysOss oss = store.findObject(ossId);
        if (oss == null || !Objects.equals(oss.getService(), item.getTargetConfigKey())) {
            throw new OssMigrationException(OssMigrationError.SERVICE_DRIFT, "对象已不在公开配置上");
        }
        if (!store.compareAndSetService(ossId, item.getTargetConfigKey(), item.getSourceConfigKey())) {
            throw new OssMigrationException(OssMigrationError.SERVICE_DRIFT, "对象存储指针已变化");
        }
        item.setStage(OssMigrationStage.ROLLED_BACK);
        item.setStatus(OssMigrationStatus.ROLLED_BACK);
        item.setErrorMessage(null);
        store.saveItem(item);
        refreshBatch(requireBatch(item.getOssMigrationBatchId()));
    }

    public DryRunReport dryRun(MigrationRequest request) {
        List<Long> ids = validateRequest(request);
        requireRoute(request.targetConfigKey(), AccessPolicy.PUBLIC_READ);
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
                requireRoute(source, AccessPolicy.PRIVATE);
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
        DryRunReport report = dryRun(request);
        if (!report.ready()) {
            throw new OssMigrationException(OssMigrationError.INVALID_REQUEST, "迁移预检未通过");
        }
        Map<Long, SysOss> objects = new LinkedHashMap<>();
        store.findObjects(report.items().stream().map(PreflightItem::ossId).toList())
            .forEach(oss -> objects.put(oss.getOssId(), oss));
        SysOssMigrationBatch batch = store.createBatch(request.targetConfigKey(), report.items().size());
        for (PreflightItem result : report.items()) {
            SysOssMigrationItem item = store.createItem(batch.getOssMigrationBatchId(),
                objects.get(result.ossId()), request.targetConfigKey());
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
                item.setRetryCount(item.getRetryCount() + 1);
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
            if (!objectStore.exists(item.getSourceConfigKey(), item.getObjectKey())) {
                fail(item, OssMigrationStage.ROLLED_BACK, OssMigrationError.OBJECT_NOT_FOUND);
                continue;
            }
            SysOss oss = store.findObject(item.getOssId());
            if (oss != null && Objects.equals(oss.getService(), item.getTargetConfigKey())
                && !store.compareAndSetService(item.getOssId(), item.getTargetConfigKey(),
                item.getSourceConfigKey())) {
                fail(item, OssMigrationStage.ROLLED_BACK, OssMigrationError.SERVICE_DRIFT);
                continue;
            }
            item.setStage(OssMigrationStage.ROLLED_BACK);
            item.setStatus(OssMigrationStatus.ROLLED_BACK);
            item.setErrorMessage(null);
            store.saveItem(item);
        }
        refreshBatch(batch);
    }

    public void cleanup(Long batchId, boolean approved) {
        if (!approved) {
            throw new OssMigrationException(OssMigrationError.CLEANUP_NOT_APPROVED, "源对象清理尚未批准");
        }
        SysOssMigrationBatch batch = requireBatch(batchId);
        List<SysOssMigrationItem> items = store.listItems(batchId);
        Instant now = clock.instant();
        if (items.stream().anyMatch(item -> item.getStatus() != OssMigrationStatus.CLEANUP_ELIGIBLE
            && item.getStatus() != OssMigrationStatus.COMPLETED
            && !(item.getStatus() == OssMigrationStatus.FAILED
            && item.getLastErrorStage() == OssMigrationStage.COMPLETED))) {
            throw new OssMigrationException(OssMigrationError.INVALID_STATE, "批次尚不可清理");
        }
        if (items.stream().filter(item -> item.getStatus() != OssMigrationStatus.COMPLETED)
            .anyMatch(item -> item.getCleanupEligibleTime() == null
            || now.isBefore(item.getCleanupEligibleTime()))) {
            throw new OssMigrationException(OssMigrationError.CLEANUP_WINDOW_OPEN, "源对象清理安全窗口尚未结束");
        }
        for (SysOssMigrationItem item : items) {
            if (item.getStatus() == OssMigrationStatus.COMPLETED) {
                continue;
            }
            try {
                objectStore.delete(item.getSourceConfigKey(), item.getObjectKey());
                item.setCleanedTime(now);
                item.setStage(OssMigrationStage.COMPLETED);
                item.setStatus(OssMigrationStatus.COMPLETED);
                item.setLastErrorStage(null);
                item.setErrorMessage(null);
                store.saveItem(item);
            } catch (RuntimeException ex) {
                fail(item, OssMigrationStage.COMPLETED, OssMigrationError.CLEANUP_FAILED);
                throw new OssMigrationException(OssMigrationError.CLEANUP_FAILED, "源对象清理失败", ex);
            }
        }
        refreshBatch(batch);
    }

    private void process(SysOssMigrationItem item) {
        int version = item.getVersion() == null ? 0 : item.getVersion();
        if (!store.claim(item.getOssMigrationItemId(), version)) {
            return;
        }
        item.setVersion(version + 1);
        try {
            item.setStage(OssMigrationStage.COPIED);
            store.saveItem(item);
            OssMigrationObjectStore.Transfer transfer = objectStore.transferAndVerify(item.getSourceConfigKey(),
                item.getTargetConfigKey(), item.getObjectKey(), properties.getMaxVerifyBytes());
            item.setSourceSize(transfer.sourceSize());
            item.setTargetSize(transfer.targetSize());
            item.setSourceEtag(transfer.sourceEtag());
            item.setTargetEtag(transfer.targetEtag());
            item.setStage(OssMigrationStage.CONTENT_VERIFIED);
            store.saveItem(item);

            SysOss current = store.findObject(item.getOssId());
            if (current == null) {
                throw new StageFailure(OssMigrationStage.SERVICE_SWITCHED, OssMigrationError.OBJECT_NOT_FOUND);
            }
            if (Objects.equals(current.getService(), item.getSourceConfigKey())) {
                if (!store.compareAndSetService(item.getOssId(), item.getSourceConfigKey(),
                    item.getTargetConfigKey())) {
                    throw new StageFailure(OssMigrationStage.SERVICE_SWITCHED, OssMigrationError.SERVICE_DRIFT);
                }
                item.setServiceSwitchedTime(clock.instant());
            } else if (!Objects.equals(current.getService(), item.getTargetConfigKey())) {
                throw new StageFailure(OssMigrationStage.SERVICE_SWITCHED, OssMigrationError.SERVICE_DRIFT);
            }
            item.setStage(OssMigrationStage.SERVICE_SWITCHED);
            store.saveItem(item);

            try {
                accessVerifier.verifyPublic(item.getOssId());
            } catch (RuntimeException ex) {
                boolean restored = store.compareAndSetService(item.getOssId(), item.getTargetConfigKey(),
                    item.getSourceConfigKey());
                SysOss restoredObject = store.findObject(item.getOssId());
                if (!restored && (restoredObject == null
                    || !Objects.equals(restoredObject.getService(), item.getSourceConfigKey()))) {
                    throw new StageFailure(OssMigrationStage.ACCESS_VERIFIED, OssMigrationError.SERVICE_DRIFT);
                }
                throw new StageFailure(OssMigrationStage.ACCESS_VERIFIED,
                    OssMigrationError.ACCESS_VERIFICATION_FAILED);
            }
            item.setStage(OssMigrationStage.ACCESS_VERIFIED);
            store.saveItem(item);
            item.setStage(OssMigrationStage.CLEANUP_ELIGIBLE);
            item.setStatus(OssMigrationStatus.CLEANUP_ELIGIBLE);
            item.setCleanupEligibleTime(clock.instant().plus(properties.getCleanupDelay()));
            item.setLastErrorStage(null);
            item.setErrorMessage(null);
            store.saveItem(item);
        } catch (StageFailure failure) {
            fail(item, failure.stage, failure.error);
        } catch (OssMigrationException ex) {
            OssMigrationStage failedStage = ex.error() == OssMigrationError.CONTENT_MISMATCH
                ? OssMigrationStage.CONTENT_VERIFIED : item.getStage();
            fail(item, failedStage, ex.error());
        } catch (RuntimeException ex) {
            fail(item, item.getStage() == null ? OssMigrationStage.COPIED : item.getStage(),
                OssMigrationError.COPY_FAILED);
        }
    }

    private void fail(SysOssMigrationItem item, OssMigrationStage stage, OssMigrationError error) {
        item.setStatus(OssMigrationStatus.FAILED);
        item.setLastErrorStage(stage);
        item.setErrorMessage(error.name());
        store.saveItem(item);
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

    private void requireRoute(String configKey, AccessPolicy expected) {
        OssStorageReadinessEntry entry = readinessRegistry.snapshot().get(configKey);
        if (entry == null || entry.status() != OssStorageReadinessEntry.Status.SERVING) {
            throw new OssMigrationException(OssMigrationError.STORAGE_NOT_SERVING, "迁移存储当前不可服务");
        }
        if (entry.accessPolicy() != expected) {
            throw new OssMigrationException(OssMigrationError.ACCESS_POLICY_MISMATCH, "迁移存储访问类型不匹配");
        }
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

    private static final class StageFailure extends RuntimeException {
        private final OssMigrationStage stage;
        private final OssMigrationError error;

        private StageFailure(OssMigrationStage stage, OssMigrationError error) {
            this.stage = stage;
            this.error = error;
        }
    }
}
