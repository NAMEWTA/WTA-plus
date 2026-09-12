package org.namewta.test.profile.contract;

import org.namewta.common.core.enums.BusinessStatusEnum;
import org.namewta.system.api.UserService;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.namewta.workflow.common.enums.TaskStatusEnum;
import org.namewta.workflow.handler.FlowProcessEventHandler;
import org.namewta.workflow.listener.WorkflowGlobalListener;
import org.namewta.workflow.service.IFlwCommonService;
import org.namewta.workflow.service.IFlwInstanceService;
import org.namewta.workflow.service.IFlwNodeExtService;
import org.namewta.workflow.service.IFlwTaskService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class WorkflowGlobalListenerContractTest {

    @Test
    void finishAcceptsFlowParamsWithoutVariables() {
        FlowProcessEventHandler events = mock(FlowProcessEventHandler.class);
        WorkflowGlobalListener listener = new WorkflowGlobalListener(
            mock(IFlwTaskService.class), mock(IFlwInstanceService.class), events,
            mock(IFlwCommonService.class), mock(IFlwNodeExtService.class), mock(UserService.class));
        Definition definition = mock(Definition.class);
        when(definition.getFlowCode()).thenReturn("profile_person_verification");
        Instance instance = mock(Instance.class);
        when(instance.getFlowStatus()).thenReturn(BusinessStatusEnum.WAITING.getStatus());
        ListenerVariable variable = new ListenerVariable()
            .setDefinition(definition)
            .setInstance(instance)
            .setNextTasks(List.of())
            .setFlowParams(new FlowParams().hisStatus(TaskStatusEnum.WAITING.getStatus()));

        assertThatCode(() -> listener.finish(variable)).doesNotThrowAnyException();
    }
}
