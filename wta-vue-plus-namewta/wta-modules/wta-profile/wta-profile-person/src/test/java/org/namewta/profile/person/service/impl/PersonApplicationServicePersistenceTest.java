package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;

import org.namewta.profile.person.domain.application.PersonPublication;
import org.namewta.profile.person.domain.exception.PersonApplicationException;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonBindingRow;
import org.namewta.profile.person.domain.model.read.PersonProfileRow;
import org.namewta.profile.person.domain.model.read.PersonSubmissionRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PersonApplicationServicePersistenceTest {

    private final PersonApplicationMapper mapper = mock(PersonApplicationMapper.class);
    private final JsonMapper jsonMapper = mock(JsonMapper.class);
    private final PersonApplicationServiceImpl service = new PersonApplicationServiceImpl(
        mapper, jsonMapper, mock(org.namewta.profile.api.material.ProfileMaterialPort.class),
        mock(PersonVerificationProviderRegistry.class), mock(PersonVerificationAttemptService.class),
        mock(org.namewta.profile.person.port.gateway.PersonWorkflowGateway.class),
        mock(org.namewta.system.api.ConfigService.class), java.time.Clock.systemUTC());

    @Test
    void rejectsSubmissionWhenTheIdentityIsEffectivelyBoundToAnotherAccount() {
        when(mapper.lockActiveProfileByIdentity("CN_RESIDENT_ID:110101199001011234"))
            .thenReturn(profile(9201L, null, "ACTIVE"));
        when(mapper.lockEffectiveBindingByProfile(9201L))
            .thenReturn(binding(9301L, 9201L, 202L, "ACTIVE"));

        assertThatThrownBy(() -> service.requireSubmissionAllowed(
            101L, 9201L, "CN_RESIDENT_ID:110101199001011234"))
            .isInstanceOf(PersonApplicationException.class)
            .hasMessage("PERSON_REBIND_CONFIRMATION_REQUIRED");
    }

    @Test
    void reauthenticationMovesTheIdentityGuardAndKeepsTheExistingProfileAndBinding() {
        Instant finished = Instant.parse("2026-09-01T12:00:00Z");
        PersonApplicationRow application = application();
        application.setTargetProfileId(9201L);
        PersonSubmissionRow submission = submission();
        submission.setTargetProfileId(9201L);
        PersonProfileRow currentProfile = profile(9201L, null, "ACTIVE");
        currentProfile.setIdentityKey("CN_RESIDENT_ID:110101199001011233");
        currentProfile.setCurrentVersionId(9251L);
        PersonBindingRow binding = binding(9301L, 9201L, 101L, "ACTIVE");
        PersonVersionRow currentVersion = new PersonVersionRow();
        currentVersion.setPersonVersionId(9251L);
        currentVersion.setVersionNo(1);

        when(mapper.lockApplicationById(9001L)).thenReturn(application);
        when(mapper.selectSubmission(9001L, 1)).thenReturn(submission);
        when(mapper.lockActiveProfileByIdentity(submission.getIdentityKey())).thenReturn(null);
        when(mapper.lockEffectiveBindingByUser(101L)).thenReturn(binding);
        when(mapper.lockActiveProfileById(9201L)).thenReturn(currentProfile);
        when(mapper.lockEffectiveBindingByProfile(9201L)).thenReturn(binding);
        when(mapper.updateIdentityGuard(9201L, submission.getIdentityKey())).thenReturn(1);
        when(mapper.selectCurrentVersionForUpdate(9201L)).thenReturn(currentVersion);
        when(mapper.supersedeVersion(9251L)).thenReturn(1);
        when(mapper.insertVersion(any())).thenReturn(1);
        when(mapper.updateProfile(any())).thenReturn(1);
        when(mapper.finishApplication(9001L, 1, 0, 1, finished)).thenReturn(1);

        PersonPublication publication = service.publishApproved(9001L, 1, finished);

        assertThat(publication.personProfileId()).isEqualTo(9201L);
        assertThat(publication.personBindingId()).isEqualTo(9301L);
        verify(mapper).updateIdentityGuard(9201L, submission.getIdentityKey());
        verify(mapper, org.mockito.Mockito.never()).insertProfile(any());
        verify(mapper, org.mockito.Mockito.never()).insertBinding(any());
    }

    @Test
    void publishesRevokedIdentityAsSuccessorFromTheImmutableSubmission() {
        Instant finished = Instant.parse("2026-09-01T12:00:00Z");
        when(mapper.lockApplicationById(9001L)).thenReturn(application());
        when(mapper.selectSubmission(9001L, 1)).thenReturn(submission());
        when(mapper.lockActiveProfileByIdentity("CN_RESIDENT_ID:110101199001011234")).thenReturn(null);
        when(mapper.lockEffectiveBindingByUser(101L)).thenReturn(null);
        when(mapper.lockLatestRevokedProfileByIdentity("CN_RESIDENT_ID:110101199001011234"))
            .thenReturn(profile(9199L, null, "REVOKED"));
        when(mapper.insertIdentityGuard(anyLong(), any(), anyLong())).thenReturn(1);
        when(mapper.insertProfile(any())).thenReturn(1);
        when(mapper.selectCurrentVersionForUpdate(anyLong())).thenReturn(null);
        when(mapper.insertVersion(any())).thenReturn(1);
        when(mapper.updateProfile(any())).thenReturn(1);
        when(mapper.insertBinding(any())).thenReturn(1);
        when(mapper.insertBindingEvent(any())).thenReturn(1);
        when(mapper.finishApplication(9001L, 1, 0, 1, finished)).thenReturn(1);

        PersonPublication publication = service.publishApproved(9001L, 1, finished);

        assertThat(publication.successorProfile()).isTrue();
        assertThat(publication.personProfileId()).isPositive();
        assertThat(publication.personVersionId()).isPositive();
        assertThat(publication.personBindingId()).isPositive();

        ArgumentCaptor<PersonProfileRow> profile = ArgumentCaptor.forClass(PersonProfileRow.class);
        verify(mapper).insertProfile(profile.capture());
        assertThat(profile.getValue().getPreviousProfileId()).isEqualTo(9199L);

        ArgumentCaptor<PersonVersionRow> version = ArgumentCaptor.forClass(PersonVersionRow.class);
        verify(mapper).insertVersion(version.capture());
        assertThat(version.getValue().getSourceType()).isEqualTo("USER_SUBMISSION");
        assertThat(version.getValue().getSourceId()).isEqualTo(9101L);
        assertThat(version.getValue().getFullName()).isEqualTo("张三");
    }

    @Test
    void failsClosedWhenTheWorkflowDecisionFenceDoesNotMove() {
        Instant occurred = Instant.parse("2026-09-01T12:00:00Z");
        when(mapper.updateWorkflowStatus(9001L, 2, "BACK", 3, occurred)).thenReturn(0);

        assertThatThrownBy(() -> service.updateWorkflowStatus(
            9001L, 2, "BACK", 3, occurred))
            .isInstanceOf(PersonApplicationException.class)
            .hasMessage("PERSON_APPLICATION_DECISION_CONFLICT");
    }

    private PersonApplicationRow application() {
        PersonApplicationRow row = new PersonApplicationRow();
        row.setPersonApplicationId(9001L);
        row.setApplicantUserId(101L);
        row.setStatus("WAITING");
        row.setProviderCode("manual");
        row.setSubmissionSeq(1);
        row.setRebindIntent("N");
        row.setDecisionVersion(0);
        row.setVersion(1);
        return row;
    }

    private PersonSubmissionRow submission() {
        PersonSubmissionRow row = new PersonSubmissionRow();
        row.setPersonSubmissionId(9101L);
        row.setPersonApplicationId(9001L);
        row.setSubmissionSeq(1);
        row.setApplicantUserId(101L);
        row.setFullName("张三");
        row.setDocumentTypeCode("CN_RESIDENT_ID");
        row.setDocumentNumber("110101199001011234");
        row.setIdentityKey("CN_RESIDENT_ID:110101199001011234");
        row.setGender("MALE");
        row.setBirthDate(LocalDate.of(1990, 1, 1));
        row.setValidFrom(LocalDate.of(2020, 1, 1));
        row.setValidUntil(LocalDate.of(2030, 1, 1));
        row.setProviderCode("manual");
        row.setRebindIntent("N");
        row.setSubmittedTime(Instant.parse("2026-09-01T11:00:00Z"));
        return row;
    }

    private PersonProfileRow profile(long profileId, Long previousProfileId, String status) {
        PersonProfileRow row = new PersonProfileRow();
        row.setPersonProfileId(profileId);
        row.setPreviousProfileId(previousProfileId);
        row.setStatus(status);
        row.setVersion(0);
        return row;
    }

    private PersonBindingRow binding(long bindingId, long profileId, long userId, String status) {
        PersonBindingRow row = new PersonBindingRow();
        row.setPersonBindingId(bindingId);
        row.setPersonProfileId(profileId);
        row.setUserId(userId);
        row.setStatus(status);
        row.setBindingVersion(1);
        return row;
    }
}
