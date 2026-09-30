package org.namewta.profile.enterprise.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.profile.enterprise.adapter.gateway.SpringEnterpriseTaskReviewGateway;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.WorkflowTaskReviewService;
import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;
import org.springframework.beans.factory.ObjectProvider;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class SpringEnterpriseTaskReviewGatewayTest {
    private final ObjectProvider<WorkflowTaskReviewService> provider = provider();
    private final WorkflowTaskReviewService reviews = mock(WorkflowTaskReviewService.class);
    private final ConfigService config = mock(ConfigService.class);
    private final SpringEnterpriseTaskReviewGateway gateway = new SpringEnterpriseTaskReviewGateway(provider, config);

    @Test
    void historyAndWritableLookupsUseTheirDistinctWorkflowAuthorizationContracts() {
        var context = new WorkflowTaskReviewContext(701L, 401L, "501", "profile_enterprise", 20L, 2, 601L);
        when(provider.getIfAvailable()).thenReturn(reviews);
        when(config.getConfigValue("profile.enterprise.flowCode")).thenReturn(" profile_enterprise ");
        when(reviews.readTaskContext(701L)).thenReturn(context);
        when(reviews.requireTaskContext(701L)).thenReturn(context);
        assertThat(gateway.context(701L, false)).isEqualTo(context);
        verify(reviews, never()).requireTaskContext(anyLong());
        assertThat(gateway.context(701L, true)).isEqualTo(context);
        verify(reviews).requireTaskContext(701L);
    }

    @Test
    void taskForAnotherBusinessFlowIsRejectedEvenWithMatchingBusinessId() {
        when(provider.getIfAvailable()).thenReturn(reviews);
        when(config.getConfigValue("profile.enterprise.flowCode")).thenReturn("profile_enterprise");
        when(reviews.readTaskContext(701L)).thenReturn(
            new WorkflowTaskReviewContext(701L, 401L, "501", "another-flow", 20L, 2, 601L));
        assertThatThrownBy(() -> gateway.context(701L, false)).hasMessage("ENTERPRISE_REVIEW_TASK_MISMATCH");
    }

    @Test
    void missingWorkflowDoesNotExposeBusinessDataThroughACompatibilityFallback() {
        assertThatThrownBy(() -> gateway.context(701L, false)).hasMessage("ENTERPRISE_WORKFLOW_UNAVAILABLE");
        verifyNoInteractions(config, reviews);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<WorkflowTaskReviewService> provider() { return mock(ObjectProvider.class); }
}
