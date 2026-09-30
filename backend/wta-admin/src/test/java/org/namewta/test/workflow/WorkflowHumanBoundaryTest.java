package org.namewta.test.workflow;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.workflow.api.WorkflowTaskReviewService;
import org.namewta.workflow.domain.bo.CompleteTaskBo;
import org.namewta.workflow.service.impl.WorkflowClientScopeService;
import org.namewta.workflow.service.impl.WorkflowHttpGuardAspect;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 人工 HTTP 不得获得可信 Java 系统办理或重写业务快照的能力。 */
@Tag("dev")
class WorkflowHumanBoundaryTest {
    @Test
    void humanCompletionRejectsHandlerAndInternalVariablesBeforeTaskService() throws Throwable {
        var scope = mock(WorkflowClientScopeService.class);
        var boundary = new WorkflowHttpGuardAspect(scope, mock(WorkflowTaskReviewService.class));
        var call = mock(ProceedingJoinPoint.class);
        var command = new CompleteTaskBo(); command.setTaskId(1L);
        when(call.getArgs()).thenReturn(new Object[]{command});
        command.setHandler("another-user");
        assertThatThrownBy(() -> boundary.humanCommand(call)).isInstanceOf(ServiceException.class);
        command.setHandler(null);
        for (String key : new String[]{"ignore", "ignoreDepute", "ignoreCooperate", "snapshotVersion", "submissionId"}) {
            command.setVariables(new HashMap<>(Map.of(key, true)));
            assertThatThrownBy(() -> boundary.humanCommand(call)).as(key).isInstanceOf(ServiceException.class);
        }
        verify(call, never()).proceed();
        command.setVariables(new HashMap<>(Map.of("approved", true)));
        boundary.humanCommand(call);
        verify(call).proceed();
        verify(scope).currentClient();
    }

    @Test
    void vendorMutationCannotBypassProjectTaskTransactionAndStateProtocol() throws Throwable {
        var boundary = new WorkflowHttpGuardAspect(mock(WorkflowClientScopeService.class), mock(WorkflowTaskReviewService.class));
        var call = mock(ProceedingJoinPoint.class);
        var signature = mock(org.aspectj.lang.Signature.class);
        when(call.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("handle");
        when(call.getArgs()).thenReturn(new Object[]{Map.of(), 1L, "PASS", "同意", null});
        assertThatThrownBy(() -> boundary.vendorEntry(call)).isInstanceOf(ServiceException.class);
        verify(call, never()).proceed();
    }

    @Test
    void explicitInitiatorClientIsNotDeserializedFromHttp() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var request = mapper.readValue("{\"businessId\":\"1\",\"flowCode\":\"demo\",\"initiatorClientPk\":99}",
            org.namewta.workflow.domain.bo.StartProcessBo.class);
        assertThat(request.getInitiatorClientPk()).isNull();
    }
}
