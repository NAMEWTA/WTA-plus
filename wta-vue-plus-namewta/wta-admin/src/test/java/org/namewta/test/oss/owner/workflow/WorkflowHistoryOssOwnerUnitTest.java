package org.namewta.test.oss.owner.workflow;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.system.api.OssService;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.namewta.workflow.domain.bo.BackProcessBo;
import org.namewta.workflow.domain.bo.CompleteTaskBo;
import org.namewta.workflow.oss.WorkflowHistoryOssOwner;
import org.namewta.workflow.service.impl.FlwInstanceServiceImpl;
import org.namewta.workflow.service.impl.FlwTaskServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class WorkflowHistoryOssOwnerUnitTest {

    @Test
    void createdHistoryUsesGeneratedHistoryIdInsteadOfRuntimeTaskId() {
        FlowHisTaskMapper mapper = mock(FlowHisTaskMapper.class);
        OssService ossService = mock(OssService.class);
        WorkflowHistoryOssOwner owner = new WorkflowHistoryOssOwner(mapper, ossService);
        FlowHisTask previous = history(900L, 100L, null, 10L);
        FlowHisTask created = history(901L, 100L, "77, 88,77", 10L);
        when(mapper.selectList(any())).thenReturn(List.of(previous), List.of(previous, created));

        WorkflowHistoryOssOwner.PendingHistoryAttachments pending = owner.capture(100L, "77,88");
        owner.reconcileCreated(pending);

        verify(ossService).reconcileReferences("flow_his_task", "901", List.of(), Set.of(77L, 88L));
        verify(ossService, never()).reconcileReferences(eq("flow_his_task"), eq("100"), any(), any());
    }

    @Test
    void missingCreatedHistoryFailsClosed() {
        FlowHisTaskMapper mapper = mock(FlowHisTaskMapper.class);
        WorkflowHistoryOssOwner owner = new WorkflowHistoryOssOwner(mapper, mock(OssService.class));
        FlowHisTask previous = history(900L, 100L, null, 10L);
        when(mapper.selectList(any())).thenReturn(List.of(previous), List.of(previous));

        WorkflowHistoryOssOwner.PendingHistoryAttachments pending = owner.capture(100L, "77");

        assertThrows(org.namewta.common.core.exception.ServiceException.class,
            () -> owner.reconcileCreated(pending));
    }

    @Test
    void malformedAttachmentInputFailsBeforeHistoryOrReferenceAccess() {
        FlowHisTaskMapper mapper = mock(FlowHisTaskMapper.class);
        OssService ossService = mock(OssService.class);
        WorkflowHistoryOssOwner owner = new WorkflowHistoryOssOwner(mapper, ossService);

        assertThrows(org.namewta.common.core.exception.ServiceException.class,
            () -> owner.capture(100L, "77,invalid"));

        verifyNoInteractions(mapper, ossService);
    }

    @Test
    void physicalInstanceCleanupReconcilesEachHistoryAttachmentToEmpty() {
        FlowHisTaskMapper mapper = mock(FlowHisTaskMapper.class);
        OssService ossService = mock(OssService.class);
        WorkflowHistoryOssOwner owner = new WorkflowHistoryOssOwner(mapper, ossService);
        FlowHisTask attached = history(901L, 100L, "77,88", 10L);
        FlowHisTask empty = history(902L, 101L, null, 10L);
        when(mapper.selectList(any())).thenReturn(List.of(empty, attached));

        owner.releaseByInstanceIds(List.of(10L));

        verify(ossService).reconcileReferences("flow_his_task", "901", Set.of(77L, 88L), List.of());
    }

    @Test
    void referenceFailurePropagatesFromCreation() {
        FlowHisTaskMapper mapper = mock(FlowHisTaskMapper.class);
        OssService ossService = mock(OssService.class);
        WorkflowHistoryOssOwner owner = new WorkflowHistoryOssOwner(mapper, ossService);
        FlowHisTask created = history(901L, 100L, "77", 10L);
        when(mapper.selectList(any())).thenReturn(List.of(), List.of(created));
        doThrow(new IllegalStateException("reference failure")).when(ossService)
            .reconcileReferences("flow_his_task", "901", List.of(), Set.of(77L));

        WorkflowHistoryOssOwner.PendingHistoryAttachments pending = owner.capture(100L, "77");

        assertThrows(IllegalStateException.class, () -> owner.reconcileCreated(pending));
    }

    @Test
    void referenceFailurePropagatesFromPhysicalCleanup() {
        FlowHisTaskMapper mapper = mock(FlowHisTaskMapper.class);
        OssService ossService = mock(OssService.class);
        WorkflowHistoryOssOwner owner = new WorkflowHistoryOssOwner(mapper, ossService);
        FlowHisTask attached = history(901L, 100L, "77", 10L);
        when(mapper.selectList(any())).thenReturn(List.of(attached));
        doThrow(new IllegalStateException("reference failure")).when(ossService)
            .reconcileReferences("flow_his_task", "901", Set.of(77L), List.of());

        assertThrows(IllegalStateException.class, () -> owner.releaseByInstanceIds(List.of(10L)));
    }

    @Test
    void affectedWorkflowBoundariesUseDynamicDatasourceTransactions() throws Exception {
        assertDynamicTransaction(FlwTaskServiceImpl.class, "completeTask", CompleteTaskBo.class);
        assertDynamicTransaction(FlwTaskServiceImpl.class, "backProcess", BackProcessBo.class);
        assertDynamicTransaction(FlwInstanceServiceImpl.class, "deleteHisByInstanceIds", Collection.class);
    }

    private void assertDynamicTransaction(Class<?> type, String methodName, Class<?>... parameterTypes)
        throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        assertNotNull(method.getAnnotation(DSTransactional.class), methodName + " must use @DSTransactional");
    }

    private FlowHisTask history(Long historyId, Long taskId, String ext, Long instanceId) {
        return new FlowHisTask()
            .setId(historyId)
            .setTaskId(taskId)
            .setExt(ext)
            .setInstanceId(instanceId);
    }
}
