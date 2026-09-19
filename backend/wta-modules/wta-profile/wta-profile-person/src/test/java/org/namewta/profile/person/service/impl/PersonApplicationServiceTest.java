package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;

import org.namewta.profile.person.domain.application.PersonDocumentTypeRule;
import org.namewta.profile.person.domain.application.PersonApplication;
import org.namewta.profile.person.domain.vo.PersonApplicationVo;
import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.application.PersonDraftUpdate;
import org.namewta.profile.person.domain.application.PersonIdentityFields;
import org.namewta.profile.person.domain.application.PersonPublication;
import org.namewta.profile.person.domain.application.PersonSubmission;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.system.api.ConfigService;
import org.namewta.profile.person.domain.application.PersonApplicationProcessCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PersonApplicationServiceTest {

    private final PersonApplicationMapper mapper = mock(PersonApplicationMapper.class);
    private final ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
    private final PersonVerificationProviderRegistry providers = mock(PersonVerificationProviderRegistry.class);
    private final PersonVerificationAttemptService attempts = mock(PersonVerificationAttemptService.class);
    private final PersonWorkflowGateway workflow = mock(PersonWorkflowGateway.class);
    private final ConfigService config = mock(ConfigService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    private PersonApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new PersonApplicationServiceImpl(
            mapper, materials, providers, attempts, workflow, config, clock));
    }

    @Test
    void savesCurrentUsersDraftWithFixedEnabledProvider() {
        when(config.getConfigValue("profile.person.provider.default")).thenReturn("manual");
        doReturn(Optional.empty()).when(service).findOpenByUserId(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        doAnswer(invocation -> application(9001L, 101L, "DRAFT", 0, 0))
            .when(service).saveDraft(eq(101L), eq("manual"), any());

        PersonApplicationVo result = service.save(101L, command(0));

        assertThat(result.personApplicationId()).isEqualTo(9001L);
        assertThat(result.providerCode()).isEqualTo("manual");
        verify(providers).requireEnabled("manual");
    }

    @Test
    void keepsTheCurrentAccountsProfileAsTheReauthenticationTargetWhenIdentityChanges() {
        when(config.getConfigValue("profile.person.provider.default")).thenReturn("manual");
        doReturn(Optional.empty()).when(service).findOpenByUserId(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        doReturn(9201L).when(service).findEffectiveProfileIdByUser(101L);
        doAnswer(invocation -> application(9001L, 101L, "DRAFT", 0, 0))
            .when(service).saveDraft(eq(101L), eq("manual"), any());

        service.save(101L, command(0));

        org.mockito.ArgumentCaptor<PersonDraftUpdate> update =
            org.mockito.ArgumentCaptor.forClass(PersonDraftUpdate.class);
        verify(service).saveDraft(eq(101L), eq("manual"), update.capture());
        assertThat(update.getValue().targetProfileId()).isEqualTo(9201L);
    }

    @Test
    void mapsProviderAvailabilityFailuresToTheApplicationContract() {
        when(config.getConfigValue("profile.person.provider.default")).thenReturn("manual");
        doReturn(Optional.empty()).when(service).findOpenByUserId(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        doThrow(new PersonVerificationException(PersonVerificationFailureCategory.DISABLED_PROVIDER,
            "provider disabled")).when(providers).requireEnabled("manual");

        assertThatThrownBy(() -> service.save(101L, command(0)))
            .isInstanceOf(PersonApplicationException.class)
            .hasMessage("PERSON_PROVIDER_UNAVAILABLE");
        verify(service, never()).saveDraft(anyLong(), any(), any());
    }

    @Test
    void submitsImmutableFieldAndMaterialSnapshotBeforeWorkflow() {
        PersonApplication application = application(9001L, 101L, "DRAFT", 0, 0);
        PersonSubmission submission = new PersonSubmission(9101L, 9001L, 1, 101L,
            application.fields(), "manual", Instant.parse("2026-09-01T12:00:00Z"));
        doReturn(application).when(service).lockOpenByUserId(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        doReturn(submission).when(service).insertSubmission(eq(application), any(), eq(1), any());
        doReturn(application(9001L, 101L, "WAITING", 1, 1)).when(service)
            .markWaiting(9001L, 1, 0, submission.submittedTime());

        PersonApplicationVo result = service.submit(101L, 0);

        assertThat(result.status()).isEqualTo("WAITING");
        verify(service).requireSubmissionAllowed(101L, application.targetProfileId(),
            application.fields().identityKey());
        verify(materials).validateRequired(new ProfileMaterialPort.MaterialOwnerKey(
            ProfileType.PERSON, ProfileMaterialPort.MaterialOwnerType.WORKING, 9001L),
            "CN_RESIDENT_ID", Set.of("ALWAYS"));
        verify(materials).snapshotImmutable(new ProfileMaterialPort.MaterialOwnerKey(
            ProfileType.PERSON, ProfileMaterialPort.MaterialOwnerType.WORKING, 9001L),
            new ProfileMaterialPort.MaterialOwnerKey(
                ProfileType.PERSON, ProfileMaterialPort.MaterialOwnerType.SUBMISSION, 9101L));
        verify(workflow).start(9001L, 9101L, 1);
    }

    @Test
    void workflowFailureDoesNotPublishAnything() {
        PersonApplication application = application(9001L, 101L, "DRAFT", 0, 0);
        PersonSubmission submission = new PersonSubmission(9101L, 9001L, 1, 101L,
            application.fields(), "manual", Instant.parse("2026-09-01T12:00:00Z"));
        doReturn(application).when(service).lockOpenByUserId(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        doReturn(submission).when(service).insertSubmission(eq(application), any(), eq(1), any());
        doReturn(application(9001L, 101L, "WAITING", 1, 1)).when(service)
            .markWaiting(anyLong(), anyInt(), anyInt(), any());
        org.mockito.Mockito.doThrow(new PersonApplicationException("PERSON_WORKFLOW_UNAVAILABLE"))
            .when(workflow).start(9001L, 9101L, 1);

        assertThatThrownBy(() -> service.submit(101L, 0))
            .hasMessage("PERSON_WORKFLOW_UNAVAILABLE");
        verify(service, never()).publishApproved(anyLong(), anyInt(), any());
    }

    @Test
    void finishesOnlyCurrentSnapshotAndIgnoresLateEvents() {
        PersonApplication waiting = application(9001L, 101L, "WAITING", 2, 3);
        when(config.getConfigValue("profile.person.flowCode")).thenReturn("profile_person_verification");
        doReturn(waiting).when(service).lockById(9001L);
        doReturn(new PersonSubmission(
            9101L, 9001L, 3, 101L, waiting.fields(), "manual",
            Instant.parse("2026-09-01T11:00:00Z"))).when(service).requireSubmission(9001L, 3);
        doReturn(new PersonPublication(9201L, 9301L, 9401L, false)).when(service)
            .publishApproved(9001L, 3, Instant.parse("2026-09-01T12:00:00Z"));

        service.handleProcess(event("finish", 3));
        service.handleProcess(event("back", 2));

        verify(service).publishApproved(9001L, 3, Instant.parse("2026-09-01T12:00:00Z"));
        verify(service, never()).updateWorkflowStatus(eq(9001L), eq(2), eq("BACK"), eq(2), any());
    }

    @Test
    void resolvesARealWorkflowTerminalFromThePersistedSnapshotFence() {
        PersonApplication waiting = application(9001L, 101L, "WAITING", 2, 3);
        when(config.getConfigValue("profile.person.flowCode")).thenReturn("profile_person_verification");
        doReturn(waiting).when(service).lockById(9001L);
        when(workflow.persistedSnapshotVersionByInstanceId(77L)).thenReturn(3);
        doReturn(new PersonSubmission(
            9101L, 9001L, 3, 101L, waiting.fields(), "manual",
            Instant.parse("2026-09-01T11:00:00Z"))).when(service).requireSubmission(9001L, 3);
        doReturn(new PersonPublication(9201L, 9301L, 9401L, false)).when(service)
            .publishApproved(9001L, 3, Instant.parse("2026-09-01T12:00:00Z"));
        PersonApplicationProcessCommand event = new PersonApplicationProcessCommand(
            77L, "9001", "profile_person_verification", "finish", "", null, clock.instant());

        service.handleProcess(event);

        verify(service).publishApproved(9001L, 3, Instant.parse("2026-09-01T12:00:00Z"));
    }

    @Test
    void ignoresLateFinishAfterTheSnapshotHasAlreadyReturnedToBack() {
        when(config.getConfigValue("profile.person.flowCode")).thenReturn("profile_person_verification");
        doReturn(application(9001L, 101L, "BACK", 3, 3)).when(service).lockById(9001L);

        service.handleProcess(event("finish", 3));

        verify(service, never()).requireSubmission(anyLong(), anyInt());
        verify(service, never()).publishApproved(anyLong(), anyInt(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BACK", "CANCEL", "INVALID", "TERMINATION"})
    void recordsEveryNonApprovalWorkflowTerminalWithoutPublishing(String status) {
        when(config.getConfigValue("profile.person.flowCode")).thenReturn("profile_person_verification");
        doReturn(application(9001L, 101L, "WAITING", 2, 1)).when(service).lockById(9001L);
        doNothing().when(service).updateWorkflowStatus(anyLong(), anyInt(), any(), anyInt(), any());

        service.handleProcess(event(status.toLowerCase(java.util.Locale.ROOT), 1));

        verify(service).updateWorkflowStatus(9001L, 1, status, 2,
            Instant.parse("2026-09-01T12:00:00Z"));
        verify(service, never()).publishApproved(anyLong(), anyInt(), any());
    }

    @Test
    void workflowReviewRejectInvalidatesTheSnapshotWithoutPublishing() {
        when(config.getConfigValue("profile.person.flowCode")).thenReturn("profile_person_verification");
        doReturn(application(9001L, 101L, "WAITING", 2, 1)).when(service).lockById(9001L);
        doNothing().when(service).updateWorkflowStatus(anyLong(), anyInt(), any(), anyInt(), any());
        PersonApplicationProcessCommand rejected = new PersonApplicationProcessCommand(
            null, "9001", "profile_person_verification", "finish", "REJECT", 1, clock.instant());

        service.handleProcess(rejected);

        verify(service).updateWorkflowStatus(9001L, 1, "INVALID", 2,
            Instant.parse("2026-09-01T12:00:00Z"));
        verify(service, never()).publishApproved(anyLong(), anyInt(), any());
    }

    @Test
    void rejectsInvalidDocumentAndValidityMatrix() {
        when(config.getConfigValue("profile.person.provider.default")).thenReturn("manual");
        doReturn(Optional.empty()).when(service).findOpenByUserId(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        PersonApplicationSaveBo invalid = new PersonApplicationSaveBo("A", "CN_RESIDENT_ID", "123",
            "MALE", LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31), 0);

        assertThatThrownBy(() -> service.save(101L, invalid))
            .hasMessage("PERSON_DOCUMENT_NUMBER_INVALID");
        verify(service, never()).saveDraft(anyLong(), any(), any());
    }

    private PersonApplicationSaveBo command(int version) {
        return new PersonApplicationSaveBo("张三", "CN_RESIDENT_ID", "110101199001011234",
            "MALE", LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1),
            LocalDate.of(2030, 1, 1), version);
    }

    private PersonApplication application(long applicationId, long userId, String status,
                                          int version, int submissionSeq) {
        PersonIdentityFields fields = PersonIdentityFields.normalize(command(version));
        return new PersonApplication(applicationId, userId, null, status, fields, "manual",
            submissionSeq, false, null, null, 0, version, null, null);
    }

    private PersonDocumentTypeRule documentType() {
        return new PersonDocumentTypeRule("CN_RESIDENT_ID", "^[0-9]{17}[0-9Xx]$", true);
    }

    private PersonApplicationProcessCommand event(String status, int snapshotVersion) {
        return new PersonApplicationProcessCommand(null, "9001", "profile_person_verification",
            status, "", snapshotVersion, clock.instant());
    }
}
