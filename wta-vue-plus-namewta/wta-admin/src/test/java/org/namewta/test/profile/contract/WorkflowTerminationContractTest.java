package org.namewta.test.profile.contract;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.lock.annotation.Lock4j;
import org.namewta.common.core.enums.BusinessStatusEnum;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.namewta.workflow.api.domain.WorkflowTerminationResult;
import org.namewta.workflow.common.enums.TaskStatusEnum;
import org.namewta.workflow.service.IFlwInstanceService;
import org.namewta.workflow.service.IFlwTaskService;
import org.namewta.workflow.service.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class WorkflowTerminationContractTest {

    @Test
    void terminatesAnActiveInstanceWithHistoryPreservingEngineOperation() {
        IFlwInstanceService instances = mock(IFlwInstanceService.class);
        IFlwTaskService tasks = mock(IFlwTaskService.class);
        TaskService engineTasks = mock(TaskService.class);
        when(instances.selectInstByBusinessId("person:41"))
            .thenReturn(instance(19L, BusinessStatusEnum.WAITING));
        when(engineTasks.terminationByInsId(
            org.mockito.ArgumentMatchers.eq(19L), org.mockito.ArgumentMatchers.any(FlowParams.class)))
            .thenReturn(instance(19L, BusinessStatusEnum.TERMINATION));
        WorkflowServiceImpl service = new WorkflowServiceImpl(instances, tasks, engineTasks);

        WorkflowTerminationResult result = service.terminateInstance("person:41", "administrator override");

        assertThat(result.status()).isEqualTo(WorkflowTerminationResult.Status.TERMINATED);
        assertThat(result.instanceId()).isEqualTo(19L);
        ArgumentCaptor<FlowParams> params = ArgumentCaptor.forClass(FlowParams.class);
        verify(engineTasks).terminationByInsId(org.mockito.ArgumentMatchers.eq(19L), params.capture());
        assertThat(params.getValue().getMessage()).isEqualTo("administrator override");
        assertThat(params.getValue().getFlowStatus()).isEqualTo(BusinessStatusEnum.TERMINATION.getStatus());
        assertThat(params.getValue().getHisStatus()).isEqualTo(TaskStatusEnum.TERMINATION.getStatus());
        assertThat(params.getValue().isIgnore()).isTrue();
    }

    @Test
    void missingOrTerminalInstanceIsAnIdempotentNoActiveResult() {
        IFlwInstanceService instances = mock(IFlwInstanceService.class);
        IFlwTaskService tasks = mock(IFlwTaskService.class);
        TaskService engineTasks = mock(TaskService.class);
        WorkflowServiceImpl service = new WorkflowServiceImpl(instances, tasks, engineTasks);

        assertThat(service.terminateInstance("missing", "override").status())
            .isEqualTo(WorkflowTerminationResult.Status.NO_ACTIVE_INSTANCE);
        for (BusinessStatusEnum status : BusinessStatusEnum.finishStatus().stream()
            .map(BusinessStatusEnum::getByStatus).toList()) {
            when(instances.selectInstByBusinessId(status.getStatus()))
                .thenReturn(instance(20L, status));
            assertThat(service.terminateInstance(status.getStatus(), "override").status())
                .isEqualTo(WorkflowTerminationResult.Status.NO_ACTIVE_INSTANCE);
        }
        verify(engineTasks, never()).terminationByInsId(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(FlowParams.class));
    }

    @Test
    void invalidInputAndEngineFailureCannotBeReportedAsSuccess() {
        IFlwInstanceService instances = mock(IFlwInstanceService.class);
        IFlwTaskService tasks = mock(IFlwTaskService.class);
        TaskService engineTasks = mock(TaskService.class);
        WorkflowServiceImpl service = new WorkflowServiceImpl(instances, tasks, engineTasks);

        assertThatThrownBy(() -> service.terminateInstance(" ", "override"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.terminateInstance("person:41", " "))
            .isInstanceOf(IllegalArgumentException.class);

        when(instances.selectInstByBusinessId("person:42"))
            .thenReturn(instance(21L, BusinessStatusEnum.WAITING));
        when(engineTasks.terminationByInsId(
            org.mockito.ArgumentMatchers.eq(21L), org.mockito.ArgumentMatchers.any(FlowParams.class)))
            .thenThrow(new IllegalStateException("engine unavailable"));
        assertThatThrownBy(() -> service.terminateInstance("person:42", "override"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("engine unavailable");
    }

    @Test
    void terminationBoundaryIsTransactionalAndSerializedByBusinessId() throws Exception {
        Method method = WorkflowServiceImpl.class.getMethod(
            "terminateInstance", String.class, String.class);

        assertThat(method.isAnnotationPresent(DSTransactional.class)).isTrue();
        assertThat(method.isAnnotationPresent(Lock4j.class)).isTrue();
        assertThat(method.getAnnotation(Lock4j.class).keys())
            .containsExactly("'workflow:terminate:' + #businessId");
    }

    private static FlowInstance instance(Long id, BusinessStatusEnum status) {
        return new FlowInstance().setId(id).setFlowStatus(status.getStatus());
    }
}
