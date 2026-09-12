package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.adapter.codec.PersonVerificationEvidenceCodec;

import org.namewta.profile.person.domain.verification.PersonApplicationVerificationState;
import org.namewta.profile.person.domain.verification.PersonProviderAttemptStatus;
import org.namewta.profile.person.domain.verification.PersonVerificationAttempt;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.profile.person.domain.verification.PersonVerificationSecurityAudit;
import org.namewta.profile.person.domain.model.read.PersonVerificationApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonVerificationAttemptRow;
import org.namewta.profile.person.mapper.PersonVerificationAttemptMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class PersonVerificationMapperFixture {

    private final PersonVerificationAttemptMapper mapper = mock(PersonVerificationAttemptMapper.class);
    private final PersonVerificationEvidenceCodec evidenceCodec =
        new PersonVerificationEvidenceCodec(JsonMapper.builder().build());
    private final List<PersonVerificationAttemptRow> attempts = new ArrayList<>();
    private final List<PersonVerificationSecurityAudit> audits = new ArrayList<>();
    private PersonApplicationVerificationState application;
    private int completeCount;

    public PersonVerificationMapperFixture(PersonApplicationVerificationState application) {
        this.application = application;
        when(mapper.lockApplication(anyLong())).thenAnswer(invocation -> applicationRow(invocation.getArgument(0)));
        when(mapper.nextAttemptNo(anyLong())).thenAnswer(invocation -> attempts.size() + 1);
        when(mapper.insertAttempt(any(PersonVerificationAttemptRow.class))).thenAnswer(invocation -> {
            attempts.add(invocation.getArgument(0));
            return 1;
        });
        when(mapper.lockByProviderRequest(anyString(), anyString())).thenAnswer(invocation -> attempts.stream()
            .filter(row -> invocation.<String>getArgument(0).equals(row.getProviderCode())
                && invocation.<String>getArgument(1).equals(row.getProviderRequestId()))
            .findFirst().orElse(null));
        when(mapper.completeAttempt(anyLong(), anyString(), any(), anyString(), any(), any()))
            .thenAnswer(invocation -> complete(invocation.getArgument(0), invocation.getArgument(1),
                invocation.getArgument(2), invocation.getArgument(3), invocation.getArgument(4),
                invocation.getArgument(5)));
        when(mapper.insertSecurityAudit(anyLong(), any(), anyString(), anyString(), any()))
            .thenAnswer(invocation -> {
                audits.add(new PersonVerificationSecurityAudit(
                    invocation.getArgument(1),
                    PersonVerificationFailureCategory.valueOf(invocation.getArgument(3)),
                    invocation.getArgument(4)));
                return 1;
            });
    }

    public PersonVerificationAttemptMapper mapper() {
        return mapper;
    }

    public PersonVerificationEvidenceCodec evidenceCodec() {
        return evidenceCodec;
    }

    public void setApplication(PersonApplicationVerificationState application) {
        this.application = application;
    }

    public void addAttempt(PersonVerificationAttempt attempt) {
        PersonVerificationAttemptRow row = new PersonVerificationAttemptRow();
        row.setVerificationAttemptId(attempt.verificationAttemptId());
        row.setApplicationId(attempt.applicationId());
        row.setSubmissionId(attempt.submissionId());
        row.setProviderCode(attempt.providerCode());
        row.setProviderRequestId(attempt.providerRequestId());
        row.setRequestFingerprint(attempt.requestFingerprint());
        row.setAttemptNo(attempt.attemptNo());
        row.setStatus(attempt.status().name());
        row.setProviderEvidenceJson(evidenceCodec.encode(
            attempt.callbackDigest(), attempt.providerEvidenceJson()));
        row.setNormalizedResultJson(attempt.normalizedResultJson());
        row.setErrorCode(attempt.errorCode());
        row.setCompletedTime(attempt.completedAt());
        attempts.add(row);
    }

    public List<PersonVerificationAttempt> attempts() {
        return attempts.stream().map(this::attempt).toList();
    }

    public List<PersonVerificationSecurityAudit> audits() {
        return List.copyOf(audits);
    }

    public int completeCount() {
        return completeCount;
    }

    private PersonVerificationApplicationRow applicationRow(long applicationId) {
        if (application.applicationId() != applicationId) {
            return null;
        }
        PersonVerificationApplicationRow row = new PersonVerificationApplicationRow();
        row.setApplicationId(application.applicationId());
        row.setSubmissionId(application.submissionId());
        row.setProviderCode(application.providerCode());
        row.setStatus(application.status());
        return row;
    }

    private int complete(long verificationAttemptId, String status, String normalizedResultJson,
                         String providerEvidenceJson, String errorCode, Instant completedTime) {
        PersonVerificationAttemptRow row = attempts.stream()
            .filter(value -> value.getVerificationAttemptId() == verificationAttemptId)
            .findFirst().orElse(null);
        if (row == null || !PersonProviderAttemptStatus.PENDING.name().equals(row.getStatus())) {
            return 0;
        }
        row.setStatus(status);
        row.setNormalizedResultJson(normalizedResultJson);
        row.setProviderEvidenceJson(providerEvidenceJson);
        row.setErrorCode(errorCode);
        row.setCompletedTime(completedTime);
        completeCount++;
        return 1;
    }

    private PersonVerificationAttempt attempt(PersonVerificationAttemptRow row) {
        PersonVerificationEvidenceCodec.DecodedEvidence evidence = evidenceCodec.decode(row.getProviderEvidenceJson());
        return new PersonVerificationAttempt(
            row.getVerificationAttemptId(), row.getApplicationId(), row.getSubmissionId(), row.getProviderCode(),
            row.getProviderRequestId(), row.getRequestFingerprint(), evidence.callbackDigest(), row.getAttemptNo(),
            PersonProviderAttemptStatus.valueOf(row.getStatus()), row.getNormalizedResultJson(),
            evidence.providerEvidenceJson(), row.getErrorCode(), row.getCompletedTime());
    }
}
