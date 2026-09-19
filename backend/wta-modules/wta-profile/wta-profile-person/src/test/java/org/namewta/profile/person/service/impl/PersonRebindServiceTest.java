package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;

import org.namewta.profile.person.domain.exception.PersonRebindException;
import org.namewta.profile.person.mapper.PersonApplicationMapper;
import org.namewta.profile.person.mapper.PersonRebindMapper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.person.domain.application.PersonDocumentTypeRule;
import org.namewta.profile.person.domain.application.PersonApplication;
import org.namewta.profile.person.domain.bo.PersonApplicationSaveBo;
import org.namewta.profile.person.domain.application.PersonIdentityFields;
import org.namewta.profile.person.domain.application.PersonSubmission;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonDocumentTypeRow;
import org.namewta.profile.person.domain.bo.PersonRebindConfirmBo;
import org.namewta.profile.person.domain.bo.PersonRebindIdentityBo;
import org.namewta.profile.person.domain.bo.PersonRebindMatchBo;
import org.namewta.profile.person.domain.bo.PersonRebindSubmitBo;
import org.namewta.system.api.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PersonRebindServiceTest {

    private final PersonRebindMapper rebindMapper = mock(PersonRebindMapper.class);
    private final PersonApplicationMapper applicationMapper = mock(PersonApplicationMapper.class);
    private final ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
    private final PersonVerificationProviderRegistry providers = mock(PersonVerificationProviderRegistry.class);
    private final PersonVerificationAttemptService attempts = mock(PersonVerificationAttemptService.class);
    private final PersonWorkflowGateway workflow = mock(PersonWorkflowGateway.class);
    private final UserService users = mock(UserService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

    private PersonRebindServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new PersonRebindServiceImpl(rebindMapper, applicationMapper,
            materials, providers, attempts, workflow, users, clock));
        doReturn(Optional.of(new PersonDocumentTypeRule("CN_RESIDENT_ID", "^[0-9]{17}[0-9Xx]$", true)))
            .when(service).findDocumentType("CN_RESIDENT_ID");
        doReturn(null).when(service).findEffectiveProfileIdByUser(101L);
    }

    @Test
    void revealsMaskedPhoneOnlyAfterAnExactCompleteMatch() {
        PersonApplication application = application(false, 0, "DRAFT");
        PersonRebindMapper.RebindCandidateRow candidate = candidate();
        doReturn(Optional.of(application)).when(service).findOpenByUserId(101L);
        doReturn(Optional.of(candidate)).when(service).findExactCandidate(application.fields());
        when(users.selectPhonenumberById(202L)).thenReturn("13800138000");

        var matched = service.match(101L, new PersonRebindMatchBo(identity()));
        var wrong = service.match(101L, new PersonRebindMatchBo(new PersonRebindIdentityBo("错误姓名", "CN_RESIDENT_ID",
            "110101199001011234", "MALE", LocalDate.of(1990, 1, 1),
            LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1))));

        assertThat(matched.status()).isEqualTo("REBIND_AVAILABLE");
        assertThat(matched.maskedPhone()).isEqualTo("138****8000");
        assertThat(wrong.status()).isEqualTo("NOT_AVAILABLE");
        assertThat(wrong.maskedPhone()).isNull();
        verify(users).selectPhonenumberById(202L);
    }

    @Test
    void shortPhoneNeverBecomesRecognizable() {
        PersonApplication application = application(false, 0, "DRAFT");
        doReturn(Optional.of(application)).when(service).findOpenByUserId(101L);
        doReturn(Optional.of(candidate())).when(service).findExactCandidate(application.fields());
        when(users.selectPhonenumberById(202L)).thenReturn("1234567");

        assertThat(service.match(101L, new PersonRebindMatchBo(identity())).maskedPhone()).isEqualTo("****");
    }

    @Test
    void confirmationFreezesTheCurrentBindingWithoutSwitchingIt() {
        PersonApplicationRow row = applicationRow();
        PersonRebindMapper.RebindCandidateRow candidate = candidate();
        doReturn(row).when(service).lockOpenApplication(101L);
        doReturn(true).when(service).same(row, fields());
        doNothing().when(service).requireApplicantUnbound(101L);
        doReturn(Optional.of(candidate)).when(service).findExactCandidate(fields());
        doReturn(candidate).when(service).lockCandidate(candidate, fields());
        doReturn(4).when(service).confirm(row, candidate, 3);
        when(users.selectPhonenumberById(202L)).thenReturn("13800138000");

        var result = service.confirm(101L, new PersonRebindConfirmBo(identity(), 3));

        assertThat(result.version()).isEqualTo(4);
        assertThat(result.maskedPhone()).isEqualTo("138****8000");
        verify(service).confirm(row, candidate, 3);
        verify(service, never()).unbindBinding(eq(202L), any());
    }

    @Test
    void submitKeepsTheOldBindingAndStartsReviewFromImmutableSnapshot() {
        PersonApplication application = application(true, 4, "DRAFT");
        PersonSubmission submission = new PersonSubmission(9101L, 9001L, 1, 101L,
            application.fields(), "manual", clock.instant());
        doReturn(application).when(service).lockOpenByUserId(101L);
        doNothing().when(service).requireApplicantUnbound(101L);
        doReturn(candidate()).when(service).requireFrozenCandidate(application);
        doReturn(submission).when(service).insertSubmission(eq(application), any(), eq(1), eq(clock.instant()));
        doReturn(application(true, 5, "WAITING")).when(service)
            .markWaiting(9001L, 1, 4, clock.instant());

        var result = service.submit(101L, new PersonRebindSubmitBo(4));

        assertThat(result.status()).isEqualTo("WAITING");
        verify(service).requireFrozenCandidate(application);
        verify(service, never()).unbindBinding(eq(202L), any());
        verify(workflow).start(9001L, 9101L, 1);
    }

    @Test
    void rejectsSubmitWhenTheFrozenBindingFenceChanged() {
        PersonApplication application = application(true, 4, "DRAFT");
        doReturn(application).when(service).lockOpenByUserId(101L);
        doNothing().when(service).requireApplicantUnbound(101L);
        doThrow(new PersonRebindException("PERSON_REBIND_BINDING_CHANGED"))
            .when(service).requireFrozenCandidate(application);

        assertThatThrownBy(() -> service.submit(101L, new PersonRebindSubmitBo(4)))
            .hasMessage("PERSON_REBIND_BINDING_CHANGED");
        verify(service, never()).insertSubmission(any(), any(), eq(1), any());
        verify(workflow, never()).start(9001L, 9101L, 1);
    }

    private PersonRebindIdentityBo identity() {
        return new PersonRebindIdentityBo("张三", "CN_RESIDENT_ID", "110101199001011234", "MALE",
            LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1));
    }

    private PersonIdentityFields fields() {
        PersonRebindIdentityBo value = identity();
        return PersonIdentityFields.normalize(new PersonApplicationSaveBo(value.fullName(), value.documentTypeCode(),
            value.documentNumber(), value.gender(), value.birthDate(), value.validFrom(), value.validUntil(), 0));
    }

    private PersonApplication application(boolean rebind, int version, String status) {
        return new PersonApplication(9001L, 101L, rebind ? 9201L : null, status, fields(), "manual", 0,
            rebind, rebind ? 9301L : null, rebind ? 2 : null, 0, version, null, null);
    }

    private PersonApplicationRow applicationRow() {
        PersonApplicationRow row = new PersonApplicationRow();
        row.setPersonApplicationId(9001L);
        row.setApplicantUserId(101L);
        row.setStatus("DRAFT");
        row.setVersion(3);
        row.setFullName(fields().fullName());
        row.setDocumentTypeCode(fields().documentTypeCode());
        row.setDocumentNumber(fields().documentNumber());
        row.setIdentityKey(fields().identityKey());
        row.setGender(fields().gender());
        row.setBirthDate(fields().birthDate());
        row.setValidFrom(fields().validFrom());
        row.setValidUntil(fields().validUntil());
        return row;
    }

    private PersonRebindMapper.RebindCandidateRow candidate() {
        PersonRebindMapper.RebindCandidateRow row = new PersonRebindMapper.RebindCandidateRow();
        row.setPersonProfileId(9201L);
        row.setPersonBindingId(9301L);
        row.setOldUserId(202L);
        row.setBindingVersion(2);
        row.setFullName(fields().fullName());
        row.setDocumentTypeCode(fields().documentTypeCode());
        row.setDocumentNumber(fields().documentNumber());
        row.setIdentityKey(fields().identityKey());
        row.setGender(fields().gender());
        row.setBirthDate(fields().birthDate());
        row.setValidFrom(fields().validFrom());
        row.setValidUntil(fields().validUntil());
        return row;
    }
}
