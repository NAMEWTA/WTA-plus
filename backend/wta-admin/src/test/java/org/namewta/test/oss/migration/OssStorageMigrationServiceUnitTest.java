package org.namewta.test.oss.migration;

import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.system.domain.SysOss;
import org.namewta.system.oss.migration.*;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.namewta.system.oss.migration.OssMigrationContracts.MigrationRequest;

@Tag("dev")
class OssStorageMigrationServiceUnitTest {

    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");
    private MemoryStore store;
    private FakeObjects objects;
    private MutableAccessVerifier accessVerifier;
    private OssStorageMigrationProperties properties;
    private OssStorageMigrationService service;

    @BeforeEach
    void setUp() {
        store = new MemoryStore();
        objects = new FakeObjects();
        accessVerifier = new MutableAccessVerifier();
        properties = new OssStorageMigrationProperties();
        properties.setCleanupDelay(Duration.ofHours(1));
        properties.setMaxVerifyBytes(1024);
        useReadiness(readiness());
        store.objects.put(10L, object(10L, "private", "docs/10.txt"));
    }

    @Test
    void missingOrExpiredDiagnosticMustNotBlockActualMigrationRoute() {
        OssStorageReadinessProperties diagnosticProperties = new OssStorageReadinessProperties();
        diagnosticProperties.setMaxSnapshotAge("P1D");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        OssStorageReadinessRegistry empty = new OssStorageReadinessRegistry(diagnosticProperties, clock);
        useReadiness(empty);

        var preflight = service.dryRun(new MigrationRequest(List.of(10L), "public"));
        assertThat(preflight.ready()).isTrue();
        assertThat(objects.copyCalls).hasValue(0);
        assertThat(store.batches).isEmpty();

        OssStorageReadinessRegistry expired = new OssStorageReadinessRegistry(diagnosticProperties, clock);
        expired.replace(Map.of(
            "private", new OssStorageReadinessEntry("private", AccessPolicy.PRIVATE, true, Set.of("TEST"),
                OssStorageReadinessEntry.Status.SERVING, OssStorageReadinessEntry.Reason.READY, NOW.minus(Duration.ofDays(2))),
            "public", new OssStorageReadinessEntry("public", AccessPolicy.PUBLIC_READ, true, Set.of("TEST"),
                OssStorageReadinessEntry.Status.SERVING, OssStorageReadinessEntry.Reason.READY, NOW.minus(Duration.ofDays(2)))
        ), Set.of("private", "public"), true);
        assertThat(expired.isServing("private")).isFalse();
        assertThat(expired.isServing("public")).isFalse();
        useReadiness(expired);

        long batchId = service.start(new MigrationRequest(List.of(10L), "public"));

        assertThat(store.objects.get(10L).getService()).isEqualTo("public");
        assertThat(service.batch(batchId).successCount()).isEqualTo(1);
        assertThat(objects.copyCalls).hasValue(1);
    }

    @Test
    void actualProviderPolicyMismatchStillFailsBeforeMigrationCopy() {
        objects.accessPolicyMismatch = true;

        var report = service.dryRun(new MigrationRequest(List.of(10L), "public"));

        assertThat(report.ready()).isFalse();
        assertThat(report.items()).singleElement()
            .extracting(OssMigrationContracts.PreflightItem::reason)
            .isEqualTo(OssMigrationError.ACCESS_POLICY_MISMATCH.name());
        assertThat(objects.copyCalls).hasValue(0);
        assertThat(store.batches).isEmpty();
    }

    @Test
    void publishThenUnpublishRestoresTheSameOssRow() {
        long batchId = service.publish(10L, "public");

        assertThat(store.objects.get(10L).getOssId()).isEqualTo(10L);
        assertThat(store.objects.get(10L).getService()).isEqualTo("public");
        assertThat(store.items.values()).singleElement().satisfies(item -> {
            assertThat(item.getStatus()).isEqualTo(OssMigrationStatus.CLEANUP_ELIGIBLE);
            assertThat(item.getSourceConfigKey()).isEqualTo("private");
        });
        assertThat(objects.deleteCalls).hasValue(0);

        service.unpublish(10L);

        assertThat(store.objects.get(10L).getOssId()).isEqualTo(10L);
        assertThat(store.objects.get(10L).getService()).isEqualTo("private");
        assertThat(store.getBatch(batchId).getStatus()).isEqualTo(OssMigrationStatus.ROLLED_BACK);
    }

    @Test
    void unpublishRejectsMissingWorkOrderPendingObjectAndMissingSource() {
        store.objects.get(10L).setDeleteState("PENDING");
        assertThatThrownBy(() -> service.publish(10L, "public"))
            .isInstanceOf(OssMigrationException.class);

        store.objects.get(10L).setDeleteState("ACTIVE");
        assertThatThrownBy(() -> service.unpublish(10L))
            .isInstanceOfSatisfying(OssMigrationException.class,
                error -> assertThat(error.error()).isEqualTo(OssMigrationError.INVALID_STATE));

        service.publish(10L, "public");
        objects.absentServices.add("private");
        assertThatThrownBy(() -> service.unpublish(10L))
            .isInstanceOfSatisfying(OssMigrationException.class,
                error -> assertThat(error.error()).isEqualTo(OssMigrationError.OBJECT_NOT_FOUND));
        assertThat(store.objects.get(10L).getService()).isEqualTo("public");
    }

    @Test
    void dryRunHasNoDatabaseOrProviderMutation() {
        var report = service.dryRun(new MigrationRequest(List.of(10L), "public"));

        assertThat(report.ready()).isTrue();
        assertThat(report.items()).singleElement().satisfies(item -> {
            assertThat(item.ossId()).isEqualTo(10L);
            assertThat(item.sourceConfigKey()).isEqualTo("private");
        });
        assertThat(store.batches).isEmpty();
        assertThat(store.items).isEmpty();
        assertThat(objects.copyCalls).hasValue(0);
        assertThat(objects.deleteCalls).hasValue(0);
    }

    @Test
    void migratesWithCopyVerifyCasAndFrozenAuditRoute() {
        long batchId = service.start(new MigrationRequest(List.of(10L), "public"));

        SysOssMigrationItem item = store.items.values().iterator().next();
        assertThat(store.objects.get(10L).getService()).isEqualTo("public");
        assertThat(item.getSourceConfigKey()).isEqualTo("private");
        assertThat(item.getTargetConfigKey()).isEqualTo("public");
        assertThat(item.getStage()).isEqualTo(OssMigrationStage.CLEANUP_ELIGIBLE);
        assertThat(item.getStatus()).isEqualTo(OssMigrationStatus.CLEANUP_ELIGIBLE);
        assertThat(item.getCleanupEligibleTime()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(objects.copyCalls).hasValue(1);
        assertThat(accessVerifier.verifiedOssIds).containsExactly(10L);
        assertThat(service.batch(batchId).successCount()).isEqualTo(1);
    }

    @Test
    void restoresSourceAfterAccessFailureAndRetriesWithoutOverwritingTarget() {
        accessVerifier.fail = true;
        long batchId = service.start(new MigrationRequest(List.of(10L), "public"));
        SysOssMigrationItem failed = store.items.values().iterator().next();

        assertThat(store.objects.get(10L).getService()).isEqualTo("private");
        assertThat(failed.getStatus()).isEqualTo(OssMigrationStatus.FAILED);
        assertThat(failed.getLastErrorStage()).isEqualTo(OssMigrationStage.ACCESS_VERIFIED);
        assertThat(failed.getErrorMessage()).doesNotContain("secret", "http");
        assertThat(objects.physicalCopies).hasValue(1);

        accessVerifier.fail = false;
        service.retry(batchId);

        assertThat(store.objects.get(10L).getService()).isEqualTo("public");
        assertThat(failed.getStatus()).isEqualTo(OssMigrationStatus.CLEANUP_ELIGIBLE);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(objects.copyCalls).hasValue(2);
        assertThat(objects.physicalCopies).hasValue(1);
    }

    @Test
    void failsClosedWhenServiceDriftsBeforeCas() {
        objects.beforeTransfer = () -> store.objects.get(10L).setService("other");

        long batchId = service.start(new MigrationRequest(List.of(10L), "public"));

        SysOssMigrationItem item = store.items.values().iterator().next();
        assertThat(item.getStatus()).isEqualTo(OssMigrationStatus.FAILED);
        assertThat(item.getLastErrorStage()).isEqualTo(OssMigrationStage.SERVICE_SWITCHED);
        assertThat(store.objects.get(10L).getService()).isEqualTo("other");
        assertThat(service.batch(batchId).failedCount()).isEqualTo(1);
    }

    @Test
    void cleanupRequiresApprovalAndDelayWhileRollbackPreservesSource() {
        long batchId = service.start(new MigrationRequest(List.of(10L), "public"));

        assertThatThrownBy(() -> service.cleanup(batchId, false))
            .isInstanceOfSatisfying(OssMigrationException.class,
                ex -> assertThat(ex.error()).isEqualTo(OssMigrationError.CLEANUP_NOT_APPROVED));
        assertThatThrownBy(() -> service.cleanup(batchId, true))
            .isInstanceOfSatisfying(OssMigrationException.class,
                ex -> assertThat(ex.error()).isEqualTo(OssMigrationError.CLEANUP_WINDOW_OPEN));
        assertThat(objects.deleteCalls).hasValue(0);

        service.rollback(batchId);
        SysOssMigrationItem item = store.items.values().iterator().next();
        assertThat(store.objects.get(10L).getService()).isEqualTo("private");
        assertThat(item.getStatus()).isEqualTo(OssMigrationStatus.ROLLED_BACK);
        assertThat(objects.deleteCalls).hasValue(0);
    }

    @Test
    void unpublishCannotRestoreSourceAfterCleanupHasEnteredProviderDelete() throws Exception {
        long batchId = service.publish(10L, "public");
        SysOssMigrationItem item = store.items.values().iterator().next();
        item.setCleanupEligibleTime(NOW);
        CountDownLatch deleteEntered = new CountDownLatch(1);
        CountDownLatch releaseDelete = new CountDownLatch(1);
        objects.beforeDelete = () -> {
            deleteEntered.countDown();
            try {
                if (!releaseDelete.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out waiting to release test deletion");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("test deletion interrupted", interrupted);
            }
        };
        ExecutorService callers = Executors.newFixedThreadPool(2);
        Future<?> cleanup = callers.submit(() -> service.cleanup(batchId, true));
        try {
            assertThat(deleteEntered.await(5, TimeUnit.SECONDS)).isTrue();
            Future<RuntimeException> restore = callers.submit(() -> {
                try {
                    service.unpublish(10L);
                    return null;
                } catch (RuntimeException rejected) {
                    return rejected;
                }
            });
            assertThat(restore.get(5, TimeUnit.SECONDS)).isInstanceOf(OssMigrationException.class);
            assertThat(store.objects.get(10L).getService()).isEqualTo("public");
        } finally {
            releaseDelete.countDown();
            try {
                cleanup.get(5, TimeUnit.SECONDS);
            } finally {
                callers.shutdownNow();
                assertThat(callers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            }
        }
        assertThat(objects.deleteCalls).hasValue(1);
        assertThat(store.objects.get(10L).getService()).isEqualTo("public");
    }

    @Test
    void contributesBothRoutesForActiveItems() {
        service.start(new MigrationRequest(List.of(10L), "public"));
        OssMigrationRequiredConfigContributor contributor = new OssMigrationRequiredConfigContributor(store);

        assertThat(contributor.requiredConfigs()).containsOnlyKeys("private", "public");
        assertThat(contributor.requiredConfigs().get("private")).containsExactly("OSS_MIGRATION");
    }

    @Test
    void rejectsObjectsThatAreAlreadyPendingDeletion() {
        store.objects.get(10L).setDeleteState("PENDING");

        var report = service.dryRun(new MigrationRequest(List.of(10L), "public"));

        assertThat(report.ready()).isFalse();
        assertThat(report.items()).singleElement()
            .extracting(OssMigrationContracts.PreflightItem::reason)
            .isEqualTo(OssMigrationError.INVALID_STATE.name());
        assertThat(objects.copyCalls).hasValue(0);
    }

    @Test
    void recordsContentVerificationFailureAtItsExactStage() {
        objects.transferError = new OssMigrationException(OssMigrationError.CONTENT_MISMATCH, "mismatch");

        service.start(new MigrationRequest(List.of(10L), "public"));

        SysOssMigrationItem item = store.items.values().iterator().next();
        assertThat(item.getStatus()).isEqualTo(OssMigrationStatus.FAILED);
        assertThat(item.getLastErrorStage()).isEqualTo(OssMigrationStage.CONTENT_VERIFIED);
        assertThat(item.getErrorMessage()).isEqualTo(OssMigrationError.CONTENT_MISMATCH.name());
        assertThat(store.objects.get(10L).getService()).isEqualTo("private");
    }

    @Test
    void cleanupFailureRequiresAnotherApprovedCleanupWithoutRecopying() {
        long batchId = service.start(new MigrationRequest(List.of(10L), "public"));
        SysOssMigrationItem item = store.items.values().iterator().next();
        item.setCleanupEligibleTime(NOW);
        objects.deleteFailures = 1;

        assertThatThrownBy(() -> service.cleanup(batchId, true))
            .isInstanceOfSatisfying(OssMigrationException.class,
                ex -> assertThat(ex.error()).isEqualTo(OssMigrationError.CLEANUP_FAILED));
        assertThat(item.getLastErrorStage()).isEqualTo(OssMigrationStage.COMPLETED);
        service.retry(batchId);
        assertThat(objects.copyCalls).hasValue(1);

        service.cleanup(batchId, true);
        assertThat(item.getStatus()).isEqualTo(OssMigrationStatus.COMPLETED);
        assertThat(objects.deleteCalls).hasValue(2);
    }

    @Test
    void validatesBoundedMigrationConfiguration() throws Exception {
        OssStorageMigrationProperties defaults = new OssStorageMigrationProperties();
        defaults.afterPropertiesSet();

        OssStorageMigrationProperties unsafeBatch = new OssStorageMigrationProperties();
        unsafeBatch.setMaxBatchSize(0);
        assertThatThrownBy(unsafeBatch::afterPropertiesSet).isInstanceOf(IllegalStateException.class);

        OssStorageMigrationProperties unsafeCleanup = new OssStorageMigrationProperties();
        unsafeCleanup.setCleanupDelay(Duration.ZERO);
        assertThatThrownBy(unsafeCleanup::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    private OssStorageReadinessRegistry readiness() {
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        properties.setMaxSnapshotAge("P1D");
        OssStorageReadinessRegistry registry = new OssStorageReadinessRegistry(properties,
            Clock.fixed(NOW, ZoneOffset.UTC));
        registry.replace(Map.of(
            "private", serving("private", AccessPolicy.PRIVATE),
            "public", serving("public", AccessPolicy.PUBLIC_READ)
        ), Set.of("private", "public"), true);
        return registry;
    }

    private void useReadiness(OssStorageReadinessRegistry readiness) {
        service = new OssStorageMigrationService(store, objects, accessVerifier, readiness, properties,
            Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private OssStorageReadinessEntry serving(String key, AccessPolicy policy) {
        return new OssStorageReadinessEntry(key, policy, true, Set.of("TEST"),
            OssStorageReadinessEntry.Status.SERVING, OssStorageReadinessEntry.Reason.READY, NOW);
    }

    private SysOss object(long id, String service, String key) {
        SysOss oss = new SysOss();
        oss.setOssId(id);
        oss.setService(service);
        oss.setFileName(key);
        oss.setOriginalName("10.txt");
        oss.setDeleteState("ACTIVE");
        return oss;
    }

    private static final class MemoryStore implements OssMigrationStore {
        private final Map<Long, SysOss> objects = new HashMap<>();
        private final Map<Long, SysOssMigrationBatch> batches = new LinkedHashMap<>();
        private final Map<Long, SysOssMigrationItem> items = new LinkedHashMap<>();
        private long sequence = 100;

        @Override public List<SysOss> findObjects(Collection<Long> ids) {
            return ids.stream().map(objects::get).filter(Objects::nonNull).toList();
        }
        @Override public SysOss findObject(Long ossId) { return objects.get(ossId); }
        @Override public SysOssMigrationBatch createBatch(String target, int total) {
            SysOssMigrationBatch batch = new SysOssMigrationBatch();
            batch.setOssMigrationBatchId(++sequence); batch.setTargetConfigKey(target);
            batch.setStatus(OssMigrationStatus.RUNNING); batch.setTotalCount(total);
            batch.setStartedTime(NOW); batches.put(batch.getOssMigrationBatchId(), batch); return batch;
        }
        @Override public SysOssMigrationItem createItem(Long batchId, SysOss oss, String target) {
            SysOssMigrationItem item = new SysOssMigrationItem();
            item.setOssMigrationItemId(++sequence); item.setOssMigrationBatchId(batchId); item.setOssId(oss.getOssId());
            item.setSourceConfigKey(oss.getService()); item.setTargetConfigKey(target); item.setObjectKey(oss.getFileName());
            item.setStatus(OssMigrationStatus.PENDING); item.setStage(OssMigrationStage.PREFLIGHT); item.setVersion(0);
            items.put(item.getOssMigrationItemId(), item); return item;
        }
        @Override public SysOssMigrationBatch getBatch(Long id) { return batches.get(id); }
        @Override public List<SysOssMigrationItem> listItems(Long batchId) {
            return items.values().stream().filter(i -> Objects.equals(i.getOssMigrationBatchId(), batchId)).toList();
        }
        @Override public boolean claim(Long itemId, int version) {
            SysOssMigrationItem item = items.get(itemId);
            if (item == null || item.getVersion() != version || item.getStatus() == OssMigrationStatus.RUNNING) return false;
            item.setVersion(version + 1); item.setStatus(OssMigrationStatus.RUNNING); return true;
        }
        @Override public void saveItem(SysOssMigrationItem item) { items.put(item.getOssMigrationItemId(), item); }
        @Override public void saveBatch(SysOssMigrationBatch batch) { batches.put(batch.getOssMigrationBatchId(), batch); }
        @Override public boolean compareAndSetService(Long ossId, String expected, String target) {
            SysOss oss = objects.get(ossId);
            if (oss == null || !Objects.equals(oss.getService(), expected)) return false;
            oss.setService(target); return true;
        }
        @Override public Set<String> activeConfigKeys() {
            Set<String> keys = new LinkedHashSet<>();
            items.values().stream().filter(i -> !i.getStatus().terminal()).forEach(i -> {
                keys.add(i.getSourceConfigKey()); keys.add(i.getTargetConfigKey());
            });
            return keys;
        }
        @Override public SysOssMigrationItem findLatestRestorable(Long ossId) {
            return items.values().stream()
                .filter(item -> Objects.equals(item.getOssId(), ossId)
                    && item.getStatus() == OssMigrationStatus.CLEANUP_ELIGIBLE)
                .max(Comparator.comparing(SysOssMigrationItem::getOssMigrationItemId))
                .orElse(null);
        }
    }

    private static final class FakeObjects implements OssMigrationObjectStore {
        private final AtomicInteger copyCalls = new AtomicInteger();
        private final AtomicInteger physicalCopies = new AtomicInteger();
        private final AtomicInteger deleteCalls = new AtomicInteger();
        private final Set<String> absentServices = new HashSet<>();
        private boolean targetExists;
        private boolean accessPolicyMismatch;
        private int deleteFailures;
        private RuntimeException transferError;
        private Runnable beforeTransfer = () -> { };
        private Runnable beforeDelete = () -> { };

        @Override public Inspection inspect(String source, String target, String key, long maxVerifyBytes) {
            if (accessPolicyMismatch) {
                throw new OssMigrationException(OssMigrationError.ACCESS_POLICY_MISMATCH,
                    "当前存储访问类型与迁移策略不一致");
            }
            return new Inspection(true, targetExists, false, 10, "etag");
        }
        @Override public Transfer transferAndVerify(String source, String target, String key, long maxVerifyBytes) {
            copyCalls.incrementAndGet(); beforeTransfer.run();
            if (transferError != null) throw transferError;
            if (!targetExists) { targetExists = true; physicalCopies.incrementAndGet(); }
            return new Transfer(10, 10, "etag", "etag");
        }
        @Override public boolean exists(String service, String key) { return !absentServices.contains(service); }
        @Override public void delete(String service, String key) {
            deleteCalls.incrementAndGet();
            beforeDelete.run();
            if (deleteFailures-- > 0) throw new IllegalStateException("provider delete failed");
        }
    }

    private static final class MutableAccessVerifier implements OssMigrationAccessVerifier {
        private final List<Long> verifiedOssIds = new ArrayList<>();
        private boolean fail;

        @Override public void verifyPublic(Long ossId) {
            verifiedOssIds.add(ossId);
            if (fail) throw new IllegalStateException("secret signed url must not leak");
        }
    }
}
