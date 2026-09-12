package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;

import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.person.domain.application.PersonRebindPublication;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.mapper.PersonRebindMapper;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonBindingEventRow;
import org.namewta.profile.person.domain.model.read.PersonBindingRow;
import org.namewta.profile.person.domain.model.read.PersonProfileRow;
import org.namewta.profile.person.domain.model.read.PersonSubmissionRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.namewta.system.api.UserService;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PersonRebindServicePersistenceTest {

    private final PersonRebindMapper mapper = mock(PersonRebindMapper.class);
    private final Instant now = Instant.parse("2026-09-01T12:00:00Z");
    private final PersonRebindServiceImpl service = new PersonRebindServiceImpl(
        mapper, mock(PersonApplicationMapper.class), JsonMapper.builder().build(), mock(ProfileMaterialPort.class),
        mock(PersonVerificationProviderRegistry.class), mock(PersonVerificationAttemptService.class),
        mock(PersonWorkflowGateway.class), mock(UserService.class),
        java.time.Clock.fixed(now, java.time.ZoneOffset.UTC));

    @Test
    void atomicallyPublishesOldUnboundAndNewActiveFromTheFrozenSnapshot() {
        PersonApplicationRow application = application();
        PersonSubmissionRow submission = submission();
        PersonProfileRow profile = profile();
        PersonBindingRow oldBinding = binding(9301L, 9201L, 202L, 2);
        PersonVersionRow current = new PersonVersionRow();
        current.setPersonVersionId(9401L);
        current.setVersionNo(1);
        when(mapper.lockApplication(9001L)).thenReturn(application);
        when(mapper.lockSubmission(9001L, 1)).thenReturn(submission);
        when(mapper.lockProfile(9201L)).thenReturn(profile);
        when(mapper.lockExpectedBinding(9301L, 9201L, 2)).thenReturn(oldBinding);
        when(mapper.lockCurrentVersion(9201L)).thenReturn(current);
        when(mapper.supersedeVersion(9401L)).thenReturn(1);
        when(mapper.insertVersion(any())).thenReturn(1);
        when(mapper.updateProfile(any())).thenReturn(1);
        when(mapper.unbind(9301L, 2, 101L, now)).thenReturn(1);
        when(mapper.insertBinding(any())).thenReturn(1);
        when(mapper.insertBindingEvent(any())).thenReturn(1);
        when(mapper.finishApplication(9001L, 1, 0, 5, now)).thenReturn(1);

        PersonRebindPublication publication =
            service.publishApprovedRebind(9001L, 1, now).orElseThrow();

        assertThat(publication.event().oldUserId()).isEqualTo(202L);
        assertThat(publication.personSubmissionId()).isEqualTo(9101L);
        ArgumentCaptor<PersonBindingEventRow> events = ArgumentCaptor.forClass(PersonBindingEventRow.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertBindingEvent(events.capture());
        assertThat(events.getAllValues()).extracting(PersonBindingEventRow::getEventType)
            .containsExactly("UNBOUND", "ACTIVE");
        assertThat(events.getAllValues()).extracting(PersonBindingEventRow::getBindingVersion)
            .containsExactly(3, 1);
    }

    @Test
    void bindingVersionRaceStopsBeforeAnyProfileOrBindingMutation() {
        when(mapper.lockApplication(9001L)).thenReturn(application());
        when(mapper.lockSubmission(9001L, 1)).thenReturn(submission());
        when(mapper.lockProfile(9201L)).thenReturn(profile());
        when(mapper.lockExpectedBinding(9301L, 9201L, 2)).thenReturn(null);

        assertThatThrownBy(() -> service.publishApprovedRebind(9001L, 1, now))
            .hasMessage("PERSON_REBIND_BINDING_CHANGED");
        verify(mapper, never()).supersedeVersion(anyLong());
        verify(mapper, never()).unbind(9301L, 2, 101L, now);
        verify(mapper, never()).insertBinding(any());
        verify(mapper, never()).finishApplication(9001L, 1, 0, 5, now);
    }

    @Test
    void ordinaryApplicationStopsBeforeSubmissionOrBindingMutation() {
        PersonApplicationRow application = application();
        application.setRebindIntent("N");
        when(mapper.lockApplication(9001L)).thenReturn(application);

        assertThat(service.publishApprovedRebind(9001L, 1, now)).isEmpty();

        verify(mapper, never()).lockSubmission(anyLong(), anyInt());
        verify(mapper, never()).supersedeVersion(anyLong());
        verify(mapper, never()).insertVersion(any());
        verify(mapper, never()).unbind(anyLong(), anyInt(), anyLong(), any());
        verify(mapper, never()).insertBinding(any());
        verify(mapper, never()).finishApplication(anyLong(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void selfUnbindAppendsAnImmutableEventAndDoesNotDeleteTheProfile() {
        PersonBindingRow binding = binding(9301L, 9201L, 101L, 2);
        when(mapper.lockEffectiveBindingByUser(101L)).thenReturn(binding);
        when(mapper.unbind(9301L, 2, 101L, now)).thenReturn(1);
        when(mapper.insertBindingEvent(any())).thenReturn(1);

        service.unbindBinding(101L, now);

        ArgumentCaptor<PersonBindingEventRow> event = ArgumentCaptor.forClass(PersonBindingEventRow.class);
        verify(mapper).insertBindingEvent(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("UNBOUND");
        assertThat(event.getValue().getBindingVersion()).isEqualTo(3);
        assertThat(event.getValue().getSourceType()).isEqualTo("SELF_SERVICE");
        verify(mapper, never()).updateProfile(any());
    }

    private PersonApplicationRow application() {
        PersonApplicationRow row = new PersonApplicationRow();
        row.setPersonApplicationId(9001L);
        row.setApplicantUserId(101L);
        row.setTargetProfileId(9201L);
        row.setStatus("WAITING");
        row.setSubmissionSeq(1);
        row.setRebindIntent("Y");
        row.setExpectedBindingId(9301L);
        row.setExpectedBindingVersion(2);
        row.setDecisionVersion(0);
        row.setVersion(5);
        copy(row);
        return row;
    }

    private PersonSubmissionRow submission() {
        PersonSubmissionRow row = new PersonSubmissionRow();
        row.setPersonSubmissionId(9101L);
        row.setPersonApplicationId(9001L);
        row.setSubmissionSeq(1);
        row.setApplicantUserId(101L);
        row.setTargetProfileId(9201L);
        row.setRebindIntent("Y");
        row.setExpectedBindingId(9301L);
        row.setExpectedBindingVersion(2);
        row.setSubmittedTime(now.minusSeconds(60));
        copy(row);
        return row;
    }

    private PersonProfileRow profile() {
        PersonProfileRow row = new PersonProfileRow();
        row.setPersonProfileId(9201L);
        row.setCurrentVersionId(9401L);
        row.setStatus("ACTIVE");
        row.setVersion(1);
        copy(row);
        return row;
    }

    private PersonBindingRow binding(long id, long profileId, long userId, int version) {
        PersonBindingRow row = new PersonBindingRow();
        row.setPersonBindingId(id);
        row.setPersonProfileId(profileId);
        row.setUserId(userId);
        row.setStatus("ACTIVE");
        row.setBindingVersion(version);
        row.setSourceType("USER_SUBMISSION");
        row.setSourceId(9000L);
        row.setBoundTime(now.minusSeconds(3600));
        return row;
    }

    private void copy(PersonApplicationRow row) {
        row.setFullName("张三");
        row.setDocumentTypeCode("CN_RESIDENT_ID");
        row.setDocumentNumber("110101199001011234");
        row.setIdentityKey("CN_RESIDENT_ID:110101199001011234");
        row.setGender("MALE");
        row.setBirthDate(LocalDate.of(1990, 1, 1));
        row.setValidFrom(LocalDate.of(2020, 1, 1));
        row.setValidUntil(LocalDate.of(2030, 1, 1));
    }

    private void copy(PersonSubmissionRow row) {
        row.setFullName("张三");
        row.setDocumentTypeCode("CN_RESIDENT_ID");
        row.setDocumentNumber("110101199001011234");
        row.setIdentityKey("CN_RESIDENT_ID:110101199001011234");
        row.setGender("MALE");
        row.setBirthDate(LocalDate.of(1990, 1, 1));
        row.setValidFrom(LocalDate.of(2020, 1, 1));
        row.setValidUntil(LocalDate.of(2030, 1, 1));
    }

    private void copy(PersonProfileRow row) {
        row.setFullName("张三");
        row.setDocumentTypeCode("CN_RESIDENT_ID");
        row.setDocumentNumber("110101199001011234");
        row.setIdentityKey("CN_RESIDENT_ID:110101199001011234");
        row.setGender("MALE");
        row.setBirthDate(LocalDate.of(1990, 1, 1));
        row.setValidFrom(LocalDate.of(2020, 1, 1));
        row.setValidUntil(LocalDate.of(2030, 1, 1));
    }
}
