package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.gateway.SpringEnterpriseWorkflowGateway;
import org.namewta.profile.enterprise.domain.exception.EnterpriseApplicationException;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.WorkflowService;
import org.namewta.workflow.api.domain.StartProcessDTO;
import org.namewta.workflow.api.event.ProcessEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class SpringEnterpriseWorkflowGatewayTest {

    private final ObjectProvider<WorkflowService> provider = mock(ObjectProvider.class);
    private final ConfigService config = mock(ConfigService.class);

    @Test
    void failsClosedWhenWorkflowIsNotAssembled() {
        when(config.getConfigValue(SpringEnterpriseWorkflowGateway.FLOW_CODE_KEY))
            .thenReturn("profile_enterprise_verification");
        when(provider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> new SpringEnterpriseWorkflowGateway(provider, config).start(91L, 92L, 3))
            .isInstanceOf(EnterpriseApplicationException.class)
            .hasMessage("ENTERPRISE_WORKFLOW_UNAVAILABLE");
    }

    @Test
    void failsClosedWhenTheConfiguredFlowCannotStart() {
        WorkflowService workflow = mock(WorkflowService.class);
        when(provider.getIfAvailable()).thenReturn(workflow);
        when(config.getConfigValue(SpringEnterpriseWorkflowGateway.FLOW_CODE_KEY))
            .thenReturn("profile_enterprise_verification");
        when(workflow.startCompleteTask(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        assertThatThrownBy(() -> new SpringEnterpriseWorkflowGateway(provider, config).start(91L, 92L, 3))
            .isInstanceOf(EnterpriseApplicationException.class)
            .hasMessage("ENTERPRISE_WORKFLOW_START_FAILED");
    }

    @Test
    void startsTheExactFlowWithApplicationBusinessIdAndSnapshotFence() {
        WorkflowService workflow = mock(WorkflowService.class);
        when(provider.getIfAvailable()).thenReturn(workflow);
        when(config.getConfigValue(SpringEnterpriseWorkflowGateway.FLOW_CODE_KEY))
            .thenReturn("profile_enterprise_verification");
        when(workflow.startCompleteTask(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        new SpringEnterpriseWorkflowGateway(provider, config).start(91L, 92L, 3);

        ArgumentCaptor<StartProcessDTO> process = ArgumentCaptor.forClass(StartProcessDTO.class);
        verify(workflow).startCompleteTask(process.capture());
        assertThat(process.getValue().getFlowCode()).isEqualTo("profile_enterprise_verification");
        assertThat(process.getValue().getBusinessId()).isEqualTo("91");
        assertThat(process.getValue().getVariables())
            .containsEntry("submissionId", 92L)
            .containsEntry("snapshotVersion", 3)
            .containsEntry("profileType", "ENTERPRISE");
    }

    @Test
    void resolvesTheSnapshotFenceFromPersistedInstanceVariables() {
        WorkflowService workflow = mock(WorkflowService.class);
        when(provider.getIfAvailable()).thenReturn(workflow);
        when(workflow.instanceVariable(77L)).thenReturn(Map.of("variableList", List.of(
            Map.of("key", "profileType", "value", "ENTERPRISE"),
            Map.of("key", "snapshotVersion", "value", 3))));
        ProcessEvent event = new ProcessEvent();
        event.setInstanceId(77L);

        Integer snapshotVersion = new SpringEnterpriseWorkflowGateway(provider, config)
            .persistedSnapshotVersion(event);

        assertThat(snapshotVersion).isEqualTo(3);
    }
}
