package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.domain.bo.PersonAdminDecisionBo;
import org.namewta.profile.person.domain.vo.PersonAccountCandidateVo;

import org.namewta.profile.person.domain.exception.PersonAdminException;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.person.domain.application.PersonPublication;
import org.namewta.profile.person.domain.application.PersonSubmission;
import org.namewta.profile.person.mapper.PersonAdminMapper;
import org.namewta.profile.person.service.IPersonApplicationService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Tag("dev")
class PersonAdminServiceTest {

    private final PersonAdminMapper mapper = mock(PersonAdminMapper.class);
    private final IPersonApplicationService applications = mock(IPersonApplicationService.class);
    private final ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
    private final WorkflowService workflow = mock(WorkflowService.class);
    private final UserService users = mock(UserService.class);
    private final PersonAdminServiceImpl service = spy(new PersonAdminServiceImpl(mapper,
        JsonMapper.builder().build(), applications, materials, workflow, users,
        Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC)));

    @Test
    void approvalTerminatesBeforePublishingAndFinalizesAdminDecision() {
        var state = new PersonAdminServiceImpl.DecisionState(11L, 22L, 3, 4);
        doReturn(state).when(service).beginDecision(11L, "APPROVED", 99L, "checked",
            Instant.parse("2026-09-02T00:00:00Z"));
        doNothing().when(service).resumeForApproval(state, 99L);
        doNothing().when(service).finalizeApproved(state, 31L, 41L, 99L, "checked",
            Instant.parse("2026-09-02T00:00:00Z"));
        when(workflow.terminateInstance("11", "checked"))
            .thenReturn(new WorkflowTerminationResult(WorkflowTerminationResult.Status.TERMINATED, 7L));
        when(applications.requireSubmission(11L, 3)).thenReturn(new PersonSubmission(22L, 11L, 3,
            77L, null, "manual", Instant.parse("2026-09-01T00:00:00Z")));
        when(applications.publishApproved(11L, 3, Instant.parse("2026-09-02T00:00:00Z")))
            .thenReturn(new PersonPublication(31L, 41L, 51L, false));

        service.decide(99L, 11L, new PersonAdminDecisionBo("APPROVED", "checked"));

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
        var state = new PersonAdminServiceImpl.DecisionState(11L, 22L, 3, 4);
        doReturn(state).when(service).beginDecision(anyLong(), anyString(), anyLong(), anyString(), any());
        when(workflow.terminateInstance("11", "checked")).thenThrow(new IllegalStateException("down"));

        assertThatThrownBy(() -> service.decide(99L, 11L,
            new PersonAdminDecisionBo("APPROVED", "checked")))
            .isInstanceOf(PersonAdminException.class).hasMessage("PERSON_ADMIN_WORKFLOW_TERMINATION_FAILED");
        verify(applications, never()).publishApproved(anyLong(), anyInt(), any());
        verify(service, never()).finalizeApproved(any(), anyLong(), anyLong(), anyLong(), anyString(), any());
    }

    @Test
    void eligibleUsersUsesTheClosedSystemSeamAndExcludesExistingPersonBindings() {
        UserDTO available = user(101L, "alice", "Alice");
        UserDTO bound = user(102L, "bob", "Bob");
        when(users.searchActiveUsers("ali", 50)).thenReturn(java.util.List.of(available, bound));
        doReturn(true).when(service).hasEffectiveBinding(102L);

        assertThat(service.eligibleUsers(" ali ")).containsExactly(
            new PersonAccountCandidateVo(101L, "alice", "Alice"));
    }

    @Test
    void missingWorkflowFailsClosedBeforeAnyAdminDecisionStateIsWritten() {
        PersonAdminServiceImpl core = spy(new PersonAdminServiceImpl(mapper, JsonMapper.builder().build(),
            applications, materials, null, users,
            Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC)));

        assertThatThrownBy(() -> core.decide(99L, 11L,
            new PersonAdminDecisionBo("APPROVED", "checked")))
            .isInstanceOf(PersonAdminException.class).hasMessage("PERSON_ADMIN_WORKFLOW_UNAVAILABLE");
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
