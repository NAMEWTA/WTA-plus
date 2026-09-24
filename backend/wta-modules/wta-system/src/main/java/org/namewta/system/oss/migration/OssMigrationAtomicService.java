package org.namewta.system.oss.migration;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.system.domain.SysOss;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * 单对象迁移的短事务。所有入口先锁对象行，再锁工单行；供应商 I/O 必须在事务外。
 */
@Service
public class OssMigrationAtomicService {
    public static final String CLEANUP_OUTCOME_UNKNOWN = "CLEANUP_OUTCOME_UNKNOWN";

    private final OssMigrationStore store;
    private final Clock clock;

    @Autowired
    public OssMigrationAtomicService(OssMigrationStore store) {
        this(store, Clock.systemUTC());
    }

    public OssMigrationAtomicService(OssMigrationStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /** 建单时先锁配置并核对预检快照，再锁对象和最新工单；配置漂移或任一行失败均回滚。 */
    @DSTransactional
    public SysOssMigrationItem createItem(Long batchId, Long ossId,
                                          OssMigrationStore.ConfigIdentity source,
                                          OssMigrationStore.ConfigIdentity target) {
        // 与配置管理相同的锁序：配置主键序 → Object → Item。
        store.lockMigrationConfigs(source, target);
        SysOss object = requireObject(ossId);
        requireActive(object);
        if (!Objects.equals(object.getService(), source.key())) {
            throw drift();
        }
        SysOssMigrationItem latest = store.findLatest(ossId);
        if (latest != null && !latest.getStatus().terminal()) {
            throw invalidState();
        }
        return store.createItem(batchId, object, target.key());
    }

    /** 锁定对象及最新工单后领取执行权；旧版本和来源清理未知栅栏不能重新领取。 */
    @DSTransactional
    public SysOssMigrationItem claim(Long itemId, int version) {
        SysOssMigrationItem hint = store.findItem(itemId);
        if (hint == null) {
            throw invalidState();
        }
        SysOss object = requireObject(hint.getOssId());
        requireActive(object);
        SysOssMigrationItem item = requireItem(itemId, hint.getOssId());
        requireObjectKey(object, item);
        SysOssMigrationItem latest = store.findLatest(item.getOssId());
        if (latest != null && !Objects.equals(latest.getOssMigrationItemId(), itemId)) {
            throw invalidState();
        }
        if (!Objects.equals(item.getVersion(), version)
            || !(item.getStatus() == OssMigrationStatus.PENDING || item.getStatus() == OssMigrationStatus.FAILED)
            || item.getLastErrorStage() == OssMigrationStage.COMPLETED) {
            throw invalidState();
        }
        OssMigrationStatus previousStatus = item.getStatus();
        if (!store.claim(itemId, version)) {
            throw invalidState();
        }
        SysOssMigrationItem claimed = requireItem(itemId, hint.getOssId());
        if (previousStatus == OssMigrationStatus.FAILED) {
            claimed.setRetryCount(claimed.getRetryCount() + 1);
            return save(claimed, version + 1, OssMigrationStatus.RUNNING);
        }
        return claimed;
    }

    /** 仅对持有最新 RUNNING 版本的工单推进复制阶段，CAS 失败回滚。 */
    @DSTransactional
    public SysOssMigrationItem copied(Long itemId, int version) {
        Locked locked = running(itemId, version);
        locked.item().setStage(OssMigrationStage.COPIED);
        return save(locked.item(), version, OssMigrationStatus.RUNNING);
    }

    /** 保存外部复制验证结果，不在事务内调用供应商。 */
    @DSTransactional
    public SysOssMigrationItem verified(Long itemId, int version, OssMigrationObjectStore.Transfer transfer) {
        Locked locked = running(itemId, version);
        SysOssMigrationItem item = locked.item();
        item.setSourceSize(transfer.sourceSize());
        item.setTargetSize(transfer.targetSize());
        item.setSourceEtag(transfer.sourceEtag());
        item.setTargetEtag(transfer.targetEtag());
        item.setStage(OssMigrationStage.CONTENT_VERIFIED);
        return save(item, version, OssMigrationStatus.RUNNING);
    }

    /** 同一短事务中先 CAS 对象指针再 CAS 工单；第二步失败连同指针一起回滚。 */
    @DSTransactional
    public SysOssMigrationItem switchToTarget(Long itemId, int version) {
        Locked locked = running(itemId, version);
        SysOssMigrationItem item = locked.item();
        if (Objects.equals(locked.object().getService(), item.getSourceConfigKey())) {
            requirePointer(item.getOssId(), item.getSourceConfigKey(), item.getTargetConfigKey());
            item.setServiceSwitchedTime(clock.instant());
        } else if (!Objects.equals(locked.object().getService(), item.getTargetConfigKey())) {
            throw drift();
        }
        item.setStage(OssMigrationStage.SERVICE_SWITCHED);
        return save(item, version, OssMigrationStatus.RUNNING);
    }

    /** 对持有执行权的工单登记访问验证及延迟清理窗口。 */
    @DSTransactional
    public SysOssMigrationItem accessVerified(Long itemId, int version, Instant eligibleTime) {
        Locked locked = running(itemId, version);
        SysOssMigrationItem item = locked.item();
        if (!Objects.equals(locked.object().getService(), item.getTargetConfigKey())) {
            throw drift();
        }
        item.setStage(OssMigrationStage.CLEANUP_ELIGIBLE);
        item.setStatus(OssMigrationStatus.CLEANUP_ELIGIBLE);
        item.setCleanupEligibleTime(eligibleTime);
        item.setLastErrorStage(null);
        item.setErrorMessage(null);
        return save(item, version, OssMigrationStatus.RUNNING);
    }

    /** 仅结算当前 RUNNING 版本；若需恢复指针，与失败记录同事务提交。 */
    @DSTransactional
    public void fail(Long itemId, int version, OssMigrationStage stage, OssMigrationError error,
                     boolean restoreTarget) {
        Locked locked = running(itemId, version);
        SysOssMigrationItem item = locked.item();
        if (restoreTarget && Objects.equals(locked.object().getService(), item.getTargetConfigKey())) {
            requirePointer(item.getOssId(), item.getTargetConfigKey(), item.getSourceConfigKey());
        } else if (restoreTarget && !Objects.equals(locked.object().getService(), item.getSourceConfigKey())) {
            throw drift();
        }
        item.setStatus(OssMigrationStatus.FAILED);
        item.setLastErrorStage(stage);
        item.setErrorMessage(error.name());
        save(item, version, OssMigrationStatus.RUNNING);
    }

    /** 锁定最新工单并核对对象后恢复来源；未知删除结果不能恢复。 */
    @DSTransactional
    public void restore(Long itemId, int version) {
        SysOssMigrationItem hint = store.findItem(itemId);
        if (hint == null) throw invalidState();
        SysOss object = requireObject(hint.getOssId());
        requireActive(object);
        SysOssMigrationItem item = requireItem(itemId, hint.getOssId());
        requireObjectKey(object, item);
        if (!Objects.equals(item.getVersion(), version) || unresolved(item)
            || item.getStatus() == OssMigrationStatus.COMPLETED
            || item.getStatus() == OssMigrationStatus.ROLLED_BACK) {
            throw invalidState();
        }
        SysOssMigrationItem latest = store.findLatest(item.getOssId());
        if (latest == null || !Objects.equals(latest.getOssMigrationItemId(), itemId)) {
            throw invalidState();
        }
        if (Objects.equals(object.getService(), item.getTargetConfigKey())) {
            requirePointer(item.getOssId(), item.getTargetConfigKey(), item.getSourceConfigKey());
        } else if (!Objects.equals(object.getService(), item.getSourceConfigKey())
            || item.getStatus() == OssMigrationStatus.CLEANUP_ELIGIBLE) {
            throw drift();
        }
        OssMigrationStatus expected = item.getStatus();
        item.setStage(OssMigrationStage.ROLLED_BACK);
        item.setStatus(OssMigrationStatus.ROLLED_BACK);
        item.setErrorMessage(null);
        item.setLastErrorStage(null);
        save(item, version, expected);
    }

    /** 先提交一次 DELETE 执行权的持久未知标记；此方法内绝不调用供应商。 */
    @DSTransactional
    public Reservation reserveCleanup(Long itemId, Instant now) {
        SysOssMigrationItem hint = store.findItem(itemId);
        if (hint == null) throw invalidState();
        SysOss object = requireObject(hint.getOssId());
        requireActive(object);
        SysOssMigrationItem item = requireItem(itemId, hint.getOssId());
        requireObjectKey(object, item);
        SysOssMigrationItem latest = store.findLatest(item.getOssId());
        if (latest == null || !Objects.equals(latest.getOssMigrationItemId(), itemId)
            || item.getStatus() != OssMigrationStatus.CLEANUP_ELIGIBLE) {
            throw invalidState();
        }
        if (item.getCleanupEligibleTime() == null || now.isBefore(item.getCleanupEligibleTime())) {
            throw new OssMigrationException(OssMigrationError.CLEANUP_WINDOW_OPEN, "源对象清理安全窗口尚未结束");
        }
        requireActive(object);
        if (!Objects.equals(object.getService(), item.getTargetConfigKey())) {
            throw drift();
        }
        item.setStatus(OssMigrationStatus.FAILED);
        item.setStage(OssMigrationStage.COMPLETED);
        item.setLastErrorStage(OssMigrationStage.COMPLETED);
        item.setErrorMessage(CLEANUP_OUTCOME_UNKNOWN);
        save(item, item.getVersion(), OssMigrationStatus.CLEANUP_ELIGIBLE);
        return reservation(item);
    }

    /** 仅在事务外 HEAD 已确认来源缺失、目标存在后，按原版本完成工单。 */
    @DSTransactional
    public void finalizeCleanup(Reservation reservation) {
        SysOss object = requireObject(reservation.ossId());
        SysOssMigrationItem item = requireItem(reservation.itemId(), reservation.ossId());
        requireObjectKey(object, item);
        SysOssMigrationItem latest = store.findLatest(reservation.ossId());
        if (latest == null || !Objects.equals(latest.getOssMigrationItemId(), reservation.itemId())
            || !Objects.equals(item.getVersion(), reservation.version()) || !unresolved(item)
            || !Objects.equals(item.getSourceConfigKey(), reservation.source())
            || !Objects.equals(item.getTargetConfigKey(), reservation.target())
            || !Objects.equals(item.getObjectKey(), reservation.key())) {
            throw invalidState();
        }
        requireActive(object);
        if (!Objects.equals(object.getService(), reservation.target())) {
            throw drift();
        }
        item.setStatus(OssMigrationStatus.COMPLETED);
        item.setStage(OssMigrationStage.COMPLETED);
        item.setLastErrorStage(null);
        item.setErrorMessage(null);
        item.setCleanedTime(clock.instant());
        save(item, reservation.version(), OssMigrationStatus.FAILED);
    }

    public record Reservation(Long itemId, Long ossId, int version, String source, String target, String key) { }

    public static boolean unresolved(SysOssMigrationItem item) {
        return item.getStatus() == OssMigrationStatus.FAILED
            && item.getLastErrorStage() == OssMigrationStage.COMPLETED
            && CLEANUP_OUTCOME_UNKNOWN.equals(item.getErrorMessage());
    }

    public static Reservation reservation(SysOssMigrationItem item) {
        return new Reservation(item.getOssMigrationItemId(), item.getOssId(), item.getVersion(),
            item.getSourceConfigKey(), item.getTargetConfigKey(), item.getObjectKey());
    }

    private Locked running(Long itemId, int version) {
        SysOssMigrationItem hint = store.findItem(itemId);
        if (hint == null) throw invalidState();
        SysOss object = requireObject(hint.getOssId());
        requireActive(object);
        SysOssMigrationItem item = requireItem(itemId, hint.getOssId());
        requireObjectKey(object, item);
        if (item.getStatus() != OssMigrationStatus.RUNNING || !Objects.equals(item.getVersion(), version)) {
            throw invalidState();
        }
        SysOssMigrationItem latest = store.findLatest(item.getOssId());
        if (latest == null || !Objects.equals(latest.getOssMigrationItemId(), itemId)) throw invalidState();
        return new Locked(object, item);
    }

    private SysOss requireObject(Long ossId) {
        SysOss object = store.lockObject(ossId);
        if (object == null) throw new OssMigrationException(OssMigrationError.OBJECT_NOT_FOUND, "对象不存在");
        return object;
    }

    private SysOssMigrationItem requireItem(Long itemId, Long ossId) {
        SysOssMigrationItem item = store.lockItem(itemId);
        if (item == null || !Objects.equals(item.getOssId(), ossId)) throw invalidState();
        return item;
    }

    private SysOssMigrationItem save(SysOssMigrationItem item, int expectedVersion, OssMigrationStatus expectedStatus) {
        if (!store.updateItem(item, expectedVersion, expectedStatus)) throw invalidState();
        item.setVersion(expectedVersion + 1);
        return item;
    }

    private void requirePointer(Long ossId, String expected, String target) {
        if (!store.compareAndSetService(ossId, expected, target)) throw drift();
    }

    private void requireObjectKey(SysOss object, SysOssMigrationItem item) {
        if (!Objects.equals(object.getFileName(), item.getObjectKey())) throw drift();
    }

    private void requireActive(SysOss object) {
        if (!"ACTIVE".equals(object.getDeleteState())) throw invalidState();
    }

    private static OssMigrationException invalidState() {
        return new OssMigrationException(OssMigrationError.INVALID_STATE, "迁移状态已变化");
    }

    private static OssMigrationException drift() {
        return new OssMigrationException(OssMigrationError.SERVICE_DRIFT, "对象存储指针已变化");
    }

    private record Locked(SysOss object, SysOssMigrationItem item) { }
}
