package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.domain.bo.EnterpriseAdminDecisionBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseAccountCandidateVo;

import org.namewta.profile.enterprise.domain.exception.EnterpriseAdminException;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.enterprise.domain.application.EnterprisePublication;
import org.namewta.profile.enterprise.domain.application.EnterpriseSubmission;
import org.namewta.profile.enterprise.mapper.EnterpriseAdminMapper;
import org.namewta.profile.enterprise.service.IEnterpriseApplicationService;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.namewta.workflow.api.WorkflowService;
import org.namewta.workflow.api.domain.WorkflowTerminationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("dev")
class EnterpriseAdminServiceTest {

    private final EnterpriseAdminMapper mapper = mock(EnterpriseAdminMapper.class);
    private final IEnterpriseApplicationService applications = mock(IEnterpriseApplicationService.class);
    private final ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
    private final WorkflowService workflow = mock(WorkflowService.class);
    private final UserService users = mock(UserService.class);
    private final ProfileService profiles = mock(ProfileService.class);
    private final EnterpriseAdminServiceImpl service = spy(new EnterpriseAdminServiceImpl(
        mapper, JsonMapper.builder().build(), applications, materials, workflow, users, profiles,
        Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC)));

    @Test
    void approvalTerminatesBeforePublishingAndFinalizesAdminDecision() {
        var state = new EnterpriseAdminServiceImpl.DecisionState(11L, 22L, 3, 4);
        doReturn(state).when(service).beginDecision(11L, "APPROVED", 99L, "checked",
            Instant.parse("2026-09-02T00:00:00Z"));
        doNothing().when(service).resumeForApproval(state, 99L);
        doNothing().when(service).finalizeApproved(state, 31L, 41L, 99L, "checked",
            Instant.parse("2026-09-02T00:00:00Z"));
        when(workflow.terminateInstance("11", "checked"))
            .thenReturn(new WorkflowTerminationResult(WorkflowTerminationResult.Status.TERMINATED, 7L));
        when(applications.requireSubmission(11L, 3)).thenReturn(new EnterpriseSubmission(22L, 11L, 3,
            77L, null, "manual", Instant.parse("2026-09-01T00:00:00Z")));
        when(applications.publishApproved(11L, 3, Instant.parse("2026-09-02T00:00:00Z")))
            .thenReturn(new EnterprisePublication(31L, 41L, 51L, false));

        service.decide(99L, 11L, new EnterpriseAdminDecisionBo("APPROVED", "checked"));

        var order = inOrder(service, workflow, applications, materials);
        order.verify(service).beginDecision(11L, "APPROVED", 99L, "checked",
            Instant.parse("2026-09-02T00:00:00Z"));
        order.verify(workflow).terminateInstance("11", "checked");
        order.verify(service).resumeForApproval(state, 99L);
        order.verify(applications).publishApproved(11L, 3, Instant.parse("2026-09-02T00:00:00Z"));
        order.verify(materials).snapshotImmutable(any(), any());
        order.verify(service).finalizeApproved(state, 31L, 41L, 99L, "checked",
            Instant.parse("2026-09-02T00:00:00Z"));
    }

    @Test
    void terminationFailureNeverPublishesOrFinalizes() {
        var state = new EnterpriseAdminServiceImpl.DecisionState(11L, 22L, 3, 4);
        doReturn(state).when(service).beginDecision(anyLong(), anyString(), anyLong(), anyString(), any());
        when(workflow.terminateInstance("11", "checked")).thenThrow(new IllegalStateException("down"));

        assertThatThrownBy(() -> service.decide(99L, 11L,
            new EnterpriseAdminDecisionBo("APPROVED", "checked")))
            .isInstanceOf(EnterpriseAdminException.class).hasMessage("ENTERPRISE_ADMIN_WORKFLOW_TERMINATION_FAILED");
        verify(applications, never()).publishApproved(anyLong(), anyInt(), any());
        verify(service, never()).finalizeApproved(any(), anyLong(), anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void eligibleUsersRequireAnActivePersonProfileAndNoEnterpriseBinding() {
        UserDTO eligible = user(201L, "owner", "Owner");
        UserDTO unverified = user(202L, "guest", "Guest");
        UserDTO occupied = user(203L, "busy", "Busy");
        when(users.searchActiveUsers("o", 50)).thenReturn(java.util.List.of(eligible, unverified, occupied));
        var verified = new ProfileBindingSummary(301L, ProfileType.PERSON, Instant.parse("2026-09-01T00:00:00Z"));
        when(profiles.findByUserIds(java.util.List.of(201L, 202L, 203L))).thenReturn(Map.of(
            201L, new ProfileSummary(201L, verified, null),
            202L, ProfileSummary.unverified(202L),
            203L, new ProfileSummary(203L, verified, null)));
        doReturn(true).when(service).hasEffectiveBinding(203L);

        assertThat(service.eligibleUsers(" o ")).containsExactly(
            new EnterpriseAccountCandidateVo(201L, "owner", "Owner"));
    }

    @Test
    void missingWorkflowFailsClosedBeforeAnyAdminDecisionStateIsWritten() {
        EnterpriseAdminServiceImpl core = spy(new EnterpriseAdminServiceImpl(mapper, JsonMapper.builder().build(),
            applications, materials, null, users, profiles,
            Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC)));

        assertThatThrownBy(() -> core.decide(99L, 11L,
            new EnterpriseAdminDecisionBo("REJECTED", "checked")))
            .isInstanceOf(EnterpriseAdminException.class).hasMessage("ENTERPRISE_ADMIN_WORKFLOW_UNAVAILABLE");
        verify(core, never()).beginDecision(anyLong(), anyString(), anyLong(), anyString(), any());
    }

    private UserDTO user(long id, String userName, String nickName) {
        UserDTO user = new UserDTO();
        user.setUserId(id);
        user.setUserName(userName);
        user.setNickName(nickName);
        user.setStatus("0");
        return user;
    }
}
