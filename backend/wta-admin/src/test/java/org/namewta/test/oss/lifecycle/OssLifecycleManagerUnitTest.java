package org.namewta.test.oss.lifecycle;

import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.namewta.system.api.OssService;
import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.domain.SysOssRef;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.namewta.system.oss.mapper.SysOssRefMapper;
import org.namewta.system.oss.provider.OssObjectStore;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OSS 生命周期协调测试。
 */
@Tag("dev")
class OssLifecycleManagerUnitTest {

    @Test
    void shouldLeaveTempOnFirstIdempotentBind() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "Y", LocalDateTime.now().plusHours(1));
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(oss);
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(0L);
        when(fixture.refMapper.existsActive(10L, "biz_contract", "100")).thenReturn(false);
        when(fixture.refMapper.restoreReference(10L, "biz_contract", "100")).thenReturn(0);
        when(fixture.refMapper.insert(any(SysOssRef.class))).thenReturn(1);
        when(fixture.ossMapper.updateLifecycle(10L, "N", null)).thenReturn(1);

        OssService.OssReferenceState state = fixture.manager.bind(10L, "biz_contract", "100");

        assertFalse(state.temporary());
        assertEquals(1L, state.referenceCount());
        assertNull(state.expireTime());
        verify(fixture.ossMapper).selectByIdForUpdate(10L);
        verify(fixture.ossMapper).updateLifecycle(10L, "N", null);
    }

    @Test
    void shouldKeepExistingBindIdempotent() {
        Fixture fixture = fixture();
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(oss(10L, "N", null));
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(1L);
        when(fixture.refMapper.existsActive(10L, "biz_contract", "100")).thenReturn(true);

        OssService.OssReferenceState state = fixture.manager.bind(10L, "biz_contract", "100");

        assertEquals(1L, state.referenceCount());
        assertFalse(state.temporary());
        verify(fixture.refMapper, never()).restoreReference(anyLong(), anyString(), anyString());
        verify(fixture.refMapper, never()).insert(any(SysOssRef.class));
        verify(fixture.ossMapper, never()).updateLifecycle(anyLong(), anyString(), any());
    }

    @Test
    void existingBindShouldCancelPendingDeletion() {
        Fixture fixture = fixture();
        SysOss pending = oss(10L, "Y", LocalDateTime.now().minusMinutes(1));
        pending.setDeleteState("PENDING");
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(pending);
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(1L);
        when(fixture.refMapper.existsActive(10L, "biz_contract", "100")).thenReturn(true);

        OssService.OssReferenceState state = fixture.manager.bind(10L, "biz_contract", "100");

        assertFalse(state.temporary());
        verify(fixture.ossMapper).updateLifecycle(10L, "N", null);
    }

    @Test
    void shouldRejectNonPhysicalReferenceBeforeDatabaseAccess() {
        Fixture fixture = fixture();

        OssLifecycleException exception = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.bind(10L, "Contract", "100"));

        assertEquals(OssLifecycleError.INVALID_REFERENCE, exception.error());
        verifyNoInteractions(fixture.ossMapper, fixture.refMapper, fixture.objectStore);
    }

    @Test
    void shouldRestartTempWindowAfterLastUnbind() {
        Fixture fixture = fixture();
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(oss(10L, "N", null));
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(1L);
        when(fixture.refMapper.existsActive(10L, "biz_contract", "100")).thenReturn(true);
        when(fixture.refMapper.deactivateReference(10L, "biz_contract", "100")).thenReturn(1);
        when(fixture.ossMapper.updateLifecycle(eq(10L), eq("Y"), any(LocalDateTime.class))).thenReturn(1);

        LocalDateTime before = LocalDateTime.now().plusHours(23);
        OssService.OssReferenceState state = fixture.manager.unbind(10L, "biz_contract", "100");

        assertTrue(state.temporary());
        assertEquals(0L, state.referenceCount());
        assertTrue(state.expireTime().isAfter(before));
        ArgumentCaptor<LocalDateTime> expiry = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(fixture.ossMapper).updateLifecycle(eq(10L), eq("Y"), expiry.capture());
        assertTrue(expiry.getValue().isAfter(before));
    }

    @Test
    void missingOrRepeatedUnbindMustNotChangeLifecycle() {
        Fixture fixture = fixture();
        LocalDateTime existingExpiry = LocalDateTime.now().plusHours(3);
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(oss(10L, "Y", existingExpiry));
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(0L);
        when(fixture.refMapper.existsActive(10L, "biz_contract", "100")).thenReturn(false);

        OssService.OssReferenceState state = fixture.manager.unbind(10L, "biz_contract", "100");

        assertTrue(state.temporary());
        assertEquals(existingExpiry, state.expireTime());
        verify(fixture.ossMapper, never()).updateLifecycle(anyLong(), anyString(), any());
    }

    @Test
    void shouldRejectDeletingReferencedObjectBeforeProviderCall() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "N", null);
        when(fixture.ossMapper.selectByIdsForUpdate(List.of(10L))).thenReturn(List.of(oss));
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(1L);

        OssLifecycleException exception = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.deleteObjects(List.of(10L)));

        assertEquals(OssLifecycleError.OBJECT_REFERENCED, exception.error());
        verifyNoInteractions(fixture.objectStore);
        verify(fixture.ossMapper, never()).deleteByIds(anyCollection());
    }

    @Test
    void manualDeleteShouldOnlyPersistPendingState() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "N", null);
        when(fixture.ossMapper.selectByIdsForUpdate(List.of(10L))).thenReturn(List.of(oss));
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(0L);

        assertTrue(fixture.manager.deleteObjects(List.of(10L)));

        verify(fixture.ossMapper).markDeletePending(eq(10L), any(LocalDateTime.class));
        verifyNoInteractions(fixture.objectStore);
        verify(fixture.ossMapper, never()).deleteByIds(anyCollection());
    }

    @Test
    void restoreWithoutReferencesStartsAFreshTemporaryRetention() {
        Fixture fixture = fixture();
        SysOss pending = oss(10L, "Y", LocalDateTime.now().minusMinutes(1));
        pending.setDeleteState("PENDING");
        when(fixture.ossMapper.selectByIdsForUpdate(List.of(10L))).thenReturn(List.of(pending));
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(0L);
        when(fixture.ossMapper.updateLifecycle(eq(10L), eq("Y"), any(LocalDateTime.class))).thenReturn(1);
        LocalDateTime before = LocalDateTime.now();

        assertTrue(fixture.manager.restoreObjects(List.of(10L)));

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<LocalDateTime> expiry = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(fixture.ossMapper).updateLifecycle(eq(10L), eq("Y"), expiry.capture());
        assertFalse(expiry.getValue().isBefore(before.plus(fixture.properties.getTempRetention())));
        assertFalse(expiry.getValue().isAfter(after.plus(fixture.properties.getTempRetention())));
        verifyNoInteractions(fixture.objectStore);
    }

    @Test
    void restoreWithReferencesReturnsBoundLifecycle() {
        Fixture fixture = fixture();
        SysOss pending = oss(10L, "Y", LocalDateTime.now());
        pending.setDeleteState("PENDING");
        when(fixture.ossMapper.selectByIdsForUpdate(List.of(10L))).thenReturn(List.of(pending));
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(2L);
        when(fixture.ossMapper.updateLifecycle(10L, "N", null)).thenReturn(1);

        assertTrue(fixture.manager.restoreObjects(List.of(10L)));

        verify(fixture.ossMapper).updateLifecycle(10L, "N", null);
        verifyNoInteractions(fixture.objectStore);
    }

    @Test
    void restoreRejectsABatchThatContainsAnObjectWhichIsNotPending() {
        Fixture fixture = fixture();
        SysOss pending = oss(10L, "Y", LocalDateTime.now());
        pending.setDeleteState("PENDING");
        SysOss active = oss(11L, "N", null);
        when(fixture.ossMapper.selectByIdsForUpdate(List.of(10L, 11L))).thenReturn(List.of(pending, active));

        OssLifecycleException exception = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.restoreObjects(List.of(10L, 11L)));

        assertEquals(OssLifecycleError.OBJECT_NOT_DELETE_PENDING, exception.error());
        verify(fixture.ossMapper, never()).updateLifecycle(anyLong(), anyString(), nullable(LocalDateTime.class));
        verifyNoInteractions(fixture.objectStore);
    }

    @Test
    void restoreRejectsMissingObjectsBeforeChangingLifecycle() {
        Fixture fixture = fixture();
        when(fixture.ossMapper.selectByIdsForUpdate(List.of(10L))).thenReturn(List.of());

        OssLifecycleException exception = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.restoreObjects(List.of(10L)));

        assertEquals(OssLifecycleError.OBJECT_NOT_FOUND, exception.error());
        verify(fixture.ossMapper, never()).updateLifecycle(anyLong(), anyString(), nullable(LocalDateTime.class));
    }

    @Test
    void shouldPersistPendingStateBeforeDeletingExpiredTempObject() {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        SysOss oss = oss(10L, "Y", now.minusMinutes(1));
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(oss);
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(0L);

        boolean pending = fixture.manager.cleanupExpired(10L, now, false);

        assertTrue(pending);
        InOrder order = inOrder(fixture.ossMapper, fixture.refMapper, fixture.objectStore);
        order.verify(fixture.ossMapper).selectByIdForUpdate(10L);
        order.verify(fixture.refMapper).countActiveByOssId(10L);
        order.verify(fixture.ossMapper).markDeletePending(10L, now);
        verifyNoInteractions(fixture.objectStore);
        verify(fixture.ossMapper, never()).deleteById(anyLong());
    }

    @Test
    void shouldDeleteProviderOnlyAfterPendingStateWasCommitted() {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        SysOss oss = oss(10L, "Y", now.minusMinutes(1));
        oss.setDeleteState("PENDING");
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(oss);
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(0L);
        when(fixture.ossMapper.deleteById(10L)).thenReturn(1);

        assertTrue(fixture.manager.cleanupExpired(10L, now, false));

        InOrder order = inOrder(fixture.ossMapper, fixture.refMapper, fixture.objectStore);
        order.verify(fixture.ossMapper).selectByIdForUpdate(10L);
        order.verify(fixture.refMapper).countActiveByOssId(10L);
        order.verify(fixture.objectStore).delete(oss);
        order.verify(fixture.ossMapper).deleteById(10L);
    }

    @Test
    void shouldNotMutateProviderOrDatabaseDuringCleanupDryRun() {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(oss(10L, "Y", now.minusMinutes(1)));
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(0L);

        assertTrue(fixture.manager.cleanupExpired(10L, now, true));

        verifyNoInteractions(fixture.objectStore);
        verify(fixture.ossMapper, never()).deleteById(anyLong());
    }

    @Test
    void shouldPreserveMetadataWhenProviderDeleteFails() {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        SysOss oss = oss(10L, "Y", now.minusMinutes(1));
        oss.setDeleteState("PENDING");
        when(fixture.ossMapper.selectByIdForUpdate(10L)).thenReturn(oss);
        when(fixture.refMapper.countActiveByOssId(10L)).thenReturn(0L);
        doThrow(new OssLifecycleException(OssLifecycleError.PROVIDER_DELETE_FAILED, "failed"))
            .when(fixture.objectStore).delete(oss);

        OssLifecycleException exception = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.cleanupExpired(10L, now, false));

        assertEquals(OssLifecycleError.PROVIDER_DELETE_FAILED, exception.error());
        verify(fixture.ossMapper, never()).deleteById(anyLong());
    }

    @Test
    void shouldReturnStructuredShortLivedDownloadAuthorization() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "N", null);
        when(fixture.ossMapper.selectById(10L)).thenReturn(oss);
        when(fixture.objectStore.accessPolicy(oss)).thenReturn(AccessPolicy.PRIVATE);
        Instant expiresAt = Instant.now().plusSeconds(120);
        when(fixture.objectStore.presign(eq(oss), eq(Duration.ofMinutes(2))))
            .thenReturn(new OssPresignedRequest("GET", "https://oss.example/signed", java.util.Map.of(), expiresAt));

        OssService.OssDownloadUrl result = fixture.manager.presignDownload(10L);

        assertEquals("https://oss.example/signed", result.url());
        assertEquals(expiresAt, result.expiresAt());
        assertEquals("document.pdf", result.fileName());
    }

    @Test
    void shouldResolvePublicStableUrlWithoutExpiryOrSigning() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "N", null);
        when(fixture.ossMapper.selectById(10L)).thenReturn(oss);
        when(fixture.objectStore.accessPolicy(oss)).thenReturn(AccessPolicy.PUBLIC_READ);
        when(fixture.objectStore.publicUrl(oss)).thenReturn("https://cdn.example.test/objects/document.pdf");

        OssService.OssAccessUrl result = fixture.manager.resolveAccessUrl(10L);

        assertEquals("PUBLIC", result.accessType());
        assertEquals("https://cdn.example.test/objects/document.pdf", result.url());
        assertNull(result.expiresAt());
        verify(fixture.objectStore, never()).presign(any(), any());
    }

    @Test
    void shouldResolvePrivateUrlWithActualProviderExpiry() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "N", null);
        Instant expiresAt = Instant.now().plusSeconds(120);
        when(fixture.ossMapper.selectById(10L)).thenReturn(oss);
        when(fixture.objectStore.accessPolicy(oss)).thenReturn(AccessPolicy.PRIVATE);
        when(fixture.objectStore.presign(oss, Duration.ofMinutes(2)))
            .thenReturn(new OssPresignedRequest("GET", "https://oss.example/signed", java.util.Map.of(), expiresAt));

        OssService.OssAccessUrl result = fixture.manager.resolveAccessUrl(10L);

        assertEquals("PRIVATE", result.accessType());
        assertEquals(expiresAt, result.expiresAt());
        assertEquals("document.pdf", result.fileName());
    }

    @Test
    void shouldRejectPublicPrivatePresignAndUnknownNamedPolicy() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "N", null);
        when(fixture.ossMapper.selectById(10L)).thenReturn(oss);
        when(fixture.objectStore.accessPolicy(oss)).thenReturn(AccessPolicy.PUBLIC_READ);

        OssLifecycleException publicError = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.presignDownload(10L));
        assertEquals(OssLifecycleError.PUBLIC_PRESIGN_FORBIDDEN, publicError.error());
        verify(fixture.objectStore, never()).presign(any(), any());

        when(fixture.objectStore.accessPolicy(oss)).thenReturn(AccessPolicy.PRIVATE);
        OssLifecycleException policyError = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.presignDownload(10L, "missing"));
        assertEquals(OssLifecycleError.DOWNLOAD_POLICY_INVALID, policyError.error());
    }

    @Test
    void shouldApplyEnabledServerNamedDownloadPolicy() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "N", null);
        OssLifecycleProperties.DownloadPolicy policy = new OssLifecycleProperties.DownloadPolicy();
        policy.setTtl(Duration.ofMinutes(5));
        fixture.properties.setDownloadPolicies(java.util.Map.of("preview", policy));
        Instant expiresAt = Instant.now().plusSeconds(300);
        when(fixture.ossMapper.selectById(10L)).thenReturn(oss);
        when(fixture.objectStore.accessPolicy(oss)).thenReturn(AccessPolicy.PRIVATE);
        when(fixture.objectStore.presign(oss, Duration.ofMinutes(5)))
            .thenReturn(new OssPresignedRequest("GET", "https://oss.example/preview", java.util.Map.of(), expiresAt));

        OssService.OssDownloadUrl result = fixture.manager.presignDownload(10L, "preview");

        assertEquals(expiresAt, result.expiresAt());
        verify(fixture.objectStore).presign(oss, Duration.ofMinutes(5));
    }

    @Test
    void missingOrExpiredDiagnosticMustNotBlockCurrentPrivateObject() {
        Instant now = Instant.parse("2026-09-23T10:00:00Z");
        OssStorageReadinessProperties properties = new OssStorageReadinessProperties();
        properties.setMaxSnapshotAge(Duration.ofMinutes(1));
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        OssStorageReadinessRegistry empty = new OssStorageReadinessRegistry(properties, clock);
        OssStorageReadinessRegistry expired = new OssStorageReadinessRegistry(properties, clock);
        expired.replace(Map.of("minio", new OssStorageReadinessEntry("minio", AccessPolicy.PRIVATE,
            true, Set.of("TEST"), OssStorageReadinessEntry.Status.SERVING,
            OssStorageReadinessEntry.Reason.READY, now.minusSeconds(120))), Set.of("minio"), true);
        assertFalse(empty.isServing("minio"));
        assertFalse(expired.isServing("minio"));

        for (OssStorageReadinessRegistry diagnostic : List.of(empty, expired)) {
            Fixture fixture = fixture(diagnostic);
            SysOss oss = oss(10L, "N", null);
            Instant expiresAt = now.plusSeconds(120);
            when(fixture.ossMapper.selectById(10L)).thenReturn(oss);
            when(fixture.objectStore.accessPolicy(oss)).thenReturn(AccessPolicy.PRIVATE);
            when(fixture.objectStore.presign(oss, Duration.ofMinutes(2)))
                .thenReturn(new OssPresignedRequest("GET", "https://oss.example/signed", Map.of(), expiresAt));

            OssService.OssAccessUrl result = fixture.manager.resolveAccessUrl(10L);

            assertEquals("PRIVATE", result.accessType());
            assertEquals(expiresAt, result.expiresAt());
            verify(fixture.objectStore).presign(oss, Duration.ofMinutes(2));
        }
    }

    @Test
    void shouldRejectDownloadWhileDeletionIsPending() {
        Fixture fixture = fixture();
        SysOss pending = oss(10L, "Y", LocalDateTime.now());
        pending.setDeleteState("PENDING");
        when(fixture.ossMapper.selectById(10L)).thenReturn(pending);

        OssLifecycleException exception = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.presignDownload(10L));

        assertEquals(OssLifecycleError.OBJECT_DELETE_PENDING, exception.error());
        verifyNoInteractions(fixture.objectStore);
    }

    @Test
    void shouldRejectMissingObjectServiceBeforeAnyProviderAccess() {
        Fixture fixture = fixture();
        SysOss oss = oss(10L, "N", null);
        oss.setService(" ");
        when(fixture.ossMapper.selectById(10L)).thenReturn(oss);

        assertThrows(OssLifecycleException.class, () -> fixture.manager.resolveAccessUrl(10L));

        verifyNoInteractions(fixture.objectStore);
    }

    @Test
    void lifecycleDefaultsRequireExplicitCleanupEnablement() {
        OssLifecycleProperties properties = new OssLifecycleProperties();

        assertEquals(Duration.ofHours(24), properties.getTempRetention());
        assertEquals(Duration.ofMinutes(2), properties.getDownloadTtl());
        assertFalse(properties.isCleanupEnabled());
        assertTrue(properties.isCleanupDryRun());
    }

    private Fixture fixture() {
        return fixture(mock(OssStorageReadinessRegistry.class));
    }

    private Fixture fixture(OssStorageReadinessRegistry readinessRegistry) {
        SysOssMapper ossMapper = mock(SysOssMapper.class);
        SysOssRefMapper refMapper = mock(SysOssRefMapper.class);
        OssObjectStore objectStore = mock(OssObjectStore.class);
        OssLifecycleProperties properties = new OssLifecycleProperties();
        return new Fixture(ossMapper, refMapper, objectStore, properties, readinessRegistry,
            new OssLifecycleManager(ossMapper, refMapper, objectStore, properties, readinessRegistry));
    }

    private SysOss oss(Long id, String temporary, LocalDateTime expireTime) {
        SysOss oss = new SysOss();
        oss.setOssId(id);
        oss.setFileName("objects/document.pdf");
        oss.setOriginalName("document.pdf");
        oss.setService("minio");
        oss.setIsTemp(temporary);
        oss.setExpireTime(expireTime);
        oss.setDeleteState("ACTIVE");
        return oss;
    }

    private record Fixture(
        SysOssMapper ossMapper,
        SysOssRefMapper refMapper,
        OssObjectStore objectStore,
        OssLifecycleProperties properties,
        OssStorageReadinessRegistry readinessRegistry,
        OssLifecycleManager manager
    ) {
    }
}
