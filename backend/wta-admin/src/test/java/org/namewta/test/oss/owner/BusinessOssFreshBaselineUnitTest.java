package org.namewta.test.oss.owner;

import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.domain.SysOssRef;
import org.namewta.system.oss.mapper.SysOssRefMapper;
import org.namewta.system.oss.provider.OssObjectStore;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 无历史数据基线下的 OSS 引用与 TEMP 清理边界。
 */
@Tag("dev")
class BusinessOssFreshBaselineUnitTest {

    @Test
    void referencedExpiredObjectIsNotADryRunCleanupCandidate() {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        when(fixture.ossMapper().selectByIdForUpdate(10L)).thenReturn(oss(10L, "Y", now.minusMinutes(1)));
        when(fixture.refMapper().countActiveByOssId(10L)).thenReturn(1L);

        assertFalse(fixture.manager().cleanupExpired(10L, now, true));

        verify(fixture.ossMapper()).updateLifecycle(10L, "N", null);
        verifyNoInteractions(fixture.objectStore());
        verify(fixture.ossMapper(), never()).markDeletePending(anyLong(), any());
        verify(fixture.ossMapper(), never()).deleteById(anyLong());
    }

    @Test
    void unreferencedExpiredTempObjectIsOnlyADryRunCandidate() {
        Fixture fixture = fixture();
        LocalDateTime now = LocalDateTime.now();
        when(fixture.ossMapper().selectByIdForUpdate(10L)).thenReturn(oss(10L, "Y", now.minusMinutes(1)));
        when(fixture.refMapper().countActiveByOssId(10L)).thenReturn(0L);

        assertTrue(fixture.manager().cleanupExpired(10L, now, true));

        verifyNoInteractions(fixture.objectStore());
        verify(fixture.ossMapper(), never()).markDeletePending(anyLong(), any());
        verify(fixture.ossMapper(), never()).deleteById(anyLong());
        verify(fixture.ossMapper(), never()).updateLifecycle(anyLong(), anyString(), any());
    }

    @Test
    void lastUnbindCreatesTempWindowThatLaterBecomesDryRunCandidate() {
        Fixture fixture = fixture();
        SysOss referenced = oss(10L, "N", null);
        when(fixture.ossMapper().selectByIdForUpdate(10L)).thenReturn(referenced);
        when(fixture.refMapper().countActiveByOssId(10L)).thenReturn(1L);
        when(fixture.refMapper().existsActive(10L, "sys_user", "100")).thenReturn(true);
        when(fixture.refMapper().deactivateReference(10L, "sys_user", "100")).thenReturn(1);

        var state = fixture.manager().unbind(10L, "sys_user", "100");
        assertTrue(state.temporary());
        assertEquals(0L, state.referenceCount());

        SysOss expired = oss(10L, "Y", state.expireTime());
        when(fixture.ossMapper().selectByIdForUpdate(10L)).thenReturn(expired);
        when(fixture.refMapper().countActiveByOssId(10L)).thenReturn(0L);
        assertTrue(fixture.manager().cleanupExpired(10L, state.expireTime().plusNanos(1), true));
        verifyNoInteractions(fixture.objectStore());
    }

    @Test
    void reconciliationMovesOnlySetDifferenceAndKeepsSharedReference() {
        Fixture fixture = fixture();
        when(fixture.ossMapper().selectByIdForUpdate(30L)).thenReturn(oss(30L, "Y", LocalDateTime.now()));
        when(fixture.refMapper().countActiveByOssId(30L)).thenReturn(0L);
        when(fixture.refMapper().existsActive(30L, "sys_notice", "200")).thenReturn(false);
        when(fixture.refMapper().restoreReference(30L, "sys_notice", "200")).thenReturn(0);
        when(fixture.refMapper().insert(any(SysOssRef.class))).thenReturn(1);
        when(fixture.ossMapper().selectByIdForUpdate(10L)).thenReturn(oss(10L, "N", null));
        when(fixture.refMapper().countActiveByOssId(10L)).thenReturn(1L);
        when(fixture.refMapper().existsActive(10L, "sys_notice", "200")).thenReturn(true);
        when(fixture.refMapper().deactivateReference(10L, "sys_notice", "200")).thenReturn(1);

        fixture.manager().reconcileReferences("sys_notice", "200", List.of(20L, 10L), List.of(30L, 20L));

        InOrder order = inOrder(fixture.ossMapper(), fixture.refMapper());
        order.verify(fixture.ossMapper()).selectByIdForUpdate(30L);
        order.verify(fixture.refMapper()).countActiveByOssId(30L);
        order.verify(fixture.ossMapper()).selectByIdForUpdate(10L);
        order.verify(fixture.refMapper()).countActiveByOssId(10L);
        verify(fixture.ossMapper(), never()).selectByIdForUpdate(20L);
    }

    @Test
    void cleanupDefaultsRemainDisabledAndDryRun() {
        OssLifecycleProperties properties = new OssLifecycleProperties();

        assertFalse(properties.isCleanupEnabled());
        assertTrue(properties.isCleanupDryRun());
    }

    private Fixture fixture() {
        SysOssMapper ossMapper = mock(SysOssMapper.class);
        SysOssRefMapper refMapper = mock(SysOssRefMapper.class);
        OssObjectStore objectStore = mock(OssObjectStore.class);
        OssLifecycleProperties properties = new OssLifecycleProperties();
        return new Fixture(ossMapper, refMapper, objectStore,
            new OssLifecycleManager(ossMapper, refMapper, objectStore, properties));
    }

    private SysOss oss(Long id, String temporary, LocalDateTime expireTime) {
        SysOss oss = new SysOss();
        oss.setOssId(id);
        oss.setFileName("objects/baseline.bin");
        oss.setOriginalName("baseline.bin");
        oss.setService("minio");
        oss.setIsTemp(temporary);
        oss.setExpireTime(expireTime);
        oss.setDeleteState("ACTIVE");
        return oss;
    }

    private record Fixture(SysOssMapper ossMapper, SysOssRefMapper refMapper,
                           OssObjectStore objectStore, OssLifecycleManager manager) {
    }
}
