package org.namewta.test.oss.lifecycle;

import org.namewta.system.domain.SysOss;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.namewta.system.oss.mapper.SysOssRefMapper;
import org.namewta.system.oss.provider.OssObjectStore;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Tag("dev")
class OssReferenceReconciliationUnitTest {

    @Test
    void shouldBindAdditionsBeforeUnbindingRemovalsInStableOrder() {
        Fixture fixture = fixture();
        stubObject(fixture, 10L);
        stubObject(fixture, 20L);
        stubObject(fixture, 30L);
        stubObject(fixture, 40L);

        fixture.manager.reconcileReferences("biz_contract", "100",
            List.of(40L, 20L, 10L, 20L), List.of(30L, 20L, 10L));

        InOrder order = inOrder(fixture.ossMapper, fixture.refMapper);
        order.verify(fixture.ossMapper).selectByIdForUpdate(30L);
        order.verify(fixture.refMapper).countActiveByOssId(30L);
        order.verify(fixture.refMapper).existsActive(30L, "biz_contract", "100");
        order.verify(fixture.ossMapper).selectByIdForUpdate(40L);
        order.verify(fixture.refMapper).countActiveByOssId(40L);
        order.verify(fixture.refMapper).existsActive(40L, "biz_contract", "100");
    }

    @Test
    void shouldDoNothingWhenNormalizedSetsAreEqual() {
        Fixture fixture = fixture();

        fixture.manager.reconcileReferences("biz_contract", "100",
            List.of(20L, 10L, 20L), List.of(10L, 20L));

        verifyNoInteractions(fixture.ossMapper, fixture.refMapper, fixture.objectStore);
    }

    @Test
    void shouldTreatNullCollectionsAsEmpty() {
        Fixture fixture = fixture();

        fixture.manager.reconcileReferences("biz_contract", "100", null, null);

        verifyNoInteractions(fixture.ossMapper, fixture.refMapper, fixture.objectStore);
    }

    @Test
    void shouldRejectInvalidIdsBeforeAnyDatabaseAccess() {
        Fixture fixture = fixture();

        OssLifecycleException exception = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.reconcileReferences("biz_contract", "100", List.of(10L), java.util.Arrays.asList(20L, null)));

        assertEquals(OssLifecycleError.INVALID_REFERENCE, exception.error());
        verifyNoInteractions(fixture.ossMapper, fixture.refMapper, fixture.objectStore);
    }

    @Test
    void shouldNotUnbindOldReferencesWhenBindingAnAdditionFails() {
        Fixture fixture = fixture();
        OssLifecycleException failure = new OssLifecycleException(
            OssLifecycleError.OBJECT_NOT_FOUND, "missing");
        when(fixture.ossMapper.selectByIdForUpdate(30L)).thenThrow(failure);

        OssLifecycleException thrown = assertThrows(OssLifecycleException.class,
            () -> fixture.manager.reconcileReferences("biz_contract", "100", List.of(40L), List.of(30L)));

        assertEquals(failure, thrown);
        verify(fixture.ossMapper, never()).selectByIdForUpdate(40L);
    }

    private void stubObject(Fixture fixture, Long ossId) {
        SysOss oss = new SysOss();
        oss.setOssId(ossId);
        oss.setIsTemp("N");
        oss.setDeleteState("ACTIVE");
        when(fixture.ossMapper.selectByIdForUpdate(ossId)).thenReturn(oss);
        when(fixture.refMapper.countActiveByOssId(ossId)).thenReturn(1L);
        when(fixture.refMapper.existsActive(ossId, "biz_contract", "100"))
            .thenReturn(ossId != 30L);
    }

    private Fixture fixture() {
        SysOssMapper ossMapper = mock(SysOssMapper.class);
        SysOssRefMapper refMapper = mock(SysOssRefMapper.class);
        OssObjectStore objectStore = mock(OssObjectStore.class);
        OssLifecycleManager manager = new OssLifecycleManager(
            ossMapper, refMapper, objectStore, new OssLifecycleProperties());
        return new Fixture(ossMapper, refMapper, objectStore, manager);
    }

    private record Fixture(
        SysOssMapper ossMapper,
        SysOssRefMapper refMapper,
        OssObjectStore objectStore,
        OssLifecycleManager manager
    ) {
    }
}
