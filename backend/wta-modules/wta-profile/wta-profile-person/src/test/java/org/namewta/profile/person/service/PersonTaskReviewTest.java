package org.namewta.profile.person.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.material.ProfileTaskMaterialPort;
import org.namewta.profile.person.controller.self.PersonTaskReviewController;
import org.namewta.profile.person.dao.PersonTaskReviewDao;
import org.namewta.profile.person.domain.bo.PersonTaskDecisionBo;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.profile.person.domain.model.read.PersonTaskReviewRow;
import org.namewta.profile.person.port.gateway.PersonTaskReviewGateway;
import org.namewta.profile.person.usecase.impl.PersonTaskReviewUseCaseImpl;
import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class PersonTaskReviewTest {
    private final PersonTaskReviewDao dao = mock(PersonTaskReviewDao.class);
    private final PersonTaskReviewGateway workflow = mock(PersonTaskReviewGateway.class);
    private final ProfileTaskMaterialPort materials = mock(ProfileTaskMaterialPort.class);
    private final PersonTaskReviewService service = new PersonTaskReviewService(dao, workflow, materials);
    private final PersonTaskReviewController controller = new PersonTaskReviewController(new PersonTaskReviewUseCaseImpl(service));
    private final MaterialOwnerKey owner = new MaterialOwnerKey(ProfileType.PERSON, MaterialOwnerType.SUBMISSION, 601L);

    @Test
    void returnPersistsTheReasonBeforeReturningToApplicantAndNeverApproves() {
        when(workflow.context(701L, true)).thenReturn(context("501", 601L, 2));
        when(dao.lockReview(501L, 601L)).thenReturn(row(601L, 2));
        when(dao.recordReturnReason(501L, 2, "请补齐材料", 101L)).thenReturn(1);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(101L);
            controller.decide(701L, new PersonTaskDecisionBo("RETURN", "  请补齐材料  ", 2));
        }
        var order = inOrder(dao, workflow);
        order.verify(dao).recordReturnReason(501L, 2, "请补齐材料", 101L);
        order.verify(workflow).returnToApplicant(701L, "请补齐材料");
        verify(workflow, never()).approve(anyLong(), anyString());
    }

    @Test
    void approveOnlyAdvancesTheAuthorizedTask() {
        when(workflow.context(701L, true)).thenReturn(context("501", 601L, 2));
        when(dao.lockReview(501L, 601L)).thenReturn(row(601L, 2));
        service.decide(101L, 701L, new PersonTaskDecisionBo("APPROVE", "通过", 2));
        verify(workflow).approve(701L, "通过");
        verify(dao, never()).recordReturnReason(anyLong(), anyInt(), anyString(), anyLong());
    }

    @Test
    void historyReadsItsOwnSnapshotButCannotBeDecided() {
        when(workflow.context(701L, false)).thenReturn(context("501", 601L, 2));
        var history = row(601L, 2);
        history.setStatus("FINISH");
        when(dao.selectReview(501L, 601L)).thenReturn(history);
        when(workflow.context(701L, true)).thenThrow(new PersonApplicationException("TASK_READ_ONLY"));
        assertThat(controller.review(701L).getData().status()).isEqualTo("FINISH");
        verify(materials).listForTask(owner, 701L);
        assertThatThrownBy(() -> service.decide(101L, 701L, new PersonTaskDecisionBo("APPROVE", "通过", 2)))
            .hasMessage("TASK_READ_ONLY");
        verify(dao, never()).lockReview(anyLong(), anyLong());
        verify(workflow, never()).approve(anyLong(), anyString());
    }

    @Test
    void taskBusinessAndSubmissionMismatchCannotReadAnyMaterials() {
        when(workflow.context(701L, false)).thenReturn(context("not-an-application", 601L, 2));
        assertThatThrownBy(() -> service.review(701L)).hasMessage("PERSON_REVIEW_TASK_MISMATCH");
        verifyNoInteractions(dao, materials);
        when(workflow.context(701L, false)).thenReturn(context("501", 601L, 2));
        when(dao.selectReview(501L, 601L)).thenReturn(row(999L, 2));
        assertThatThrownBy(() -> service.review(701L)).hasMessage("PERSON_REVIEW_SNAPSHOT_CONFLICT");
        verifyNoInteractions(materials);
    }

    @Test
    void taskSequenceAndBrowserSequenceMustBothMatchTheBoundSubmission() {
        when(workflow.context(701L, false)).thenReturn(context("501", 601L, 2));
        when(dao.selectReview(501L, 601L)).thenReturn(row(601L, 3));
        assertThatThrownBy(() -> service.review(701L)).hasMessage("PERSON_REVIEW_SNAPSHOT_CONFLICT");
        when(workflow.context(701L, true)).thenReturn(context("501", 601L, 2));
        when(dao.lockReview(501L, 601L)).thenReturn(row(601L, 2));
        assertThatThrownBy(() -> service.decide(101L, 701L, new PersonTaskDecisionBo("APPROVE", "通过", 1)))
            .hasMessage("PERSON_REVIEW_SNAPSHOT_CONFLICT");
        verify(workflow, never()).approve(anyLong(), anyString());
        verifyNoInteractions(materials);
    }

    @Test
    void lostReturnRaceStopsBeforeChangingWorkflow() {
        when(workflow.context(701L, true)).thenReturn(context("501", 601L, 2));
        when(dao.lockReview(501L, 601L)).thenReturn(row(601L, 2));
        assertThatThrownBy(() -> service.decide(101L, 701L, new PersonTaskDecisionBo("RETURN", "补充", 2)))
            .hasMessage("PERSON_REVIEW_SNAPSHOT_CONFLICT");
        verify(workflow, never()).returnToApplicant(anyLong(), anyString());
    }

    @Test
    void invalidCommandsFailBeforeReadingTheTask() {
        for (var command : new PersonTaskDecisionBo[] {
            new PersonTaskDecisionBo("PASS", "通过", 2), new PersonTaskDecisionBo("RETURN", "  ", 2),
            new PersonTaskDecisionBo("APPROVE", "a".repeat(501), 2)}) {
            assertThatThrownBy(() -> service.decide(101L, 701L, command)).hasMessage("PERSON_REVIEW_DECISION_INVALID");
        }
        verifyNoInteractions(dao, workflow, materials);
    }

    @Test
    void materialAccessCarriesTheTaskAndItsSubmissionToTheAuthorizationPort() {
        when(workflow.context(701L, false)).thenReturn(context("501", 601L, 2));
        when(dao.selectReview(501L, 601L)).thenReturn(row(601L, 2));
        when(materials.accessUrlForTask(owner, 801L, 701L)).thenReturn(
            new ProfileTaskMaterialPort.MaterialAccessUrl("PRIVATE", "https://files.example.test/signed", null, "证件.png"));
        assertThat(controller.material(701L, 801L).getData().fileName()).isEqualTo("证件.png");
        verify(materials).accessUrlForTask(owner, 801L, 701L);
    }

    private WorkflowTaskReviewContext context(String businessId, Long submissionId, Integer sequence) {
        return new WorkflowTaskReviewContext(701L, 401L, businessId, "profile_person_verification", 20L, sequence, submissionId);
    }

    private PersonTaskReviewRow row(long submission, int sequence) {
        var row = new PersonTaskReviewRow();
        row.setApplicationId(501L);
        row.setApplicantUserId(102L);
        row.setStatus("WAITING");
        row.setSubmissionId(submission);
        row.setSubmissionSeq(sequence);
        row.setVersion(3);
        row.setDecisionVersion(0);
        row.setFieldSnapshotJson("{\"snapshot\":true}");
        return row;
    }
}
