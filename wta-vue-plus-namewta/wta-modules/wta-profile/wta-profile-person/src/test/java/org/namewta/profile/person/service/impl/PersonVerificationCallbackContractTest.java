package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.service.impl.PersonVerificationSecurityAuditRecorder;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;
import org.namewta.profile.person.service.impl.PersonVerificationSecurityAuditRecorder;

import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonApplicationVerificationState;
import org.namewta.profile.person.domain.verification.PersonProviderAttemptStatus;
import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonVerificationAttempt;
import org.namewta.profile.person.dao.PersonVerificationAttemptDao;
import org.namewta.profile.person.domain.verification.PersonVerificationCallbackOutcome;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.profile.person.config.PersonVerificationProviderProperties;
import org.namewta.profile.person.support.PersonVerificationTimeSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class PersonVerificationCallbackContractTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Test
    void authenticatesCallbacksAndAuditsForgedOrExpiredPayloads() {
        Fixture fixture = fixture("WAITING");

        PersonVerificationException forged = assertThrows(PersonVerificationException.class,
            () -> fixture.coordinator.handleCallback("test-provider",
                new PersonProviderCallbackEnvelope("person-41-1", NOW.getEpochSecond(), "approved", "00"), NOW));
        PersonVerificationException expired = assertThrows(PersonVerificationException.class,
            () -> fixture.coordinator.handleCallback("test-provider",
                fixture.callback("approved", NOW.minusSeconds(301)), NOW));

        assertEquals(PersonVerificationFailureCategory.INVALID_SIGNATURE, forged.category());
        assertEquals(PersonVerificationFailureCategory.EXPIRED_CALLBACK, expired.category());
        assertEquals(List.of(
                PersonVerificationFailureCategory.INVALID_SIGNATURE,
                PersonVerificationFailureCategory.EXPIRED_CALLBACK),
            fixture.mapperFixture.audits().stream()
                .map(org.namewta.profile.person.domain.verification.PersonVerificationSecurityAudit::category)
                .toList());
        assertEquals(0, fixture.mapperFixture.completeCount());
    }

    @Test
    void acceptsOnceReturnsIdempotentlyAndAuditsConflictingRedelivery() {
        Fixture fixture = fixture("WAITING");
        PersonProviderCallbackEnvelope accepted = fixture.callback("approved", NOW);

        assertEquals(PersonVerificationCallbackOutcome.ACCEPTED,
            fixture.coordinator.handleCallback("test-provider", accepted, NOW));
        assertEquals(PersonVerificationCallbackOutcome.IDEMPOTENT,
            fixture.coordinator.handleCallback("test-provider", accepted, NOW.plusSeconds(1)));
        PersonVerificationException conflict = assertThrows(PersonVerificationException.class,
            () -> fixture.coordinator.handleCallback(
                "test-provider", fixture.callback("rejected", NOW.plusSeconds(2)), NOW.plusSeconds(2)));

        assertEquals(PersonVerificationFailureCategory.CONFLICTING_CALLBACK, conflict.category());
        assertEquals(1, fixture.mapperFixture.completeCount());
        assertEquals(List.of(PersonVerificationFailureCategory.CONFLICTING_CALLBACK),
            fixture.mapperFixture.audits().stream()
                .map(org.namewta.profile.person.domain.verification.PersonVerificationSecurityAudit::category)
                .toList());
    }

    @Test
    void lateCallbackOnlyAppendsSecurityAudit() {
        Fixture fixture = fixture("FINISH");

        PersonVerificationCallbackOutcome outcome = fixture.coordinator.handleCallback(
            "test-provider", fixture.callback("approved", NOW), NOW);

        assertEquals(PersonVerificationCallbackOutcome.LATE_IGNORED, outcome);
        assertEquals(0, fixture.mapperFixture.completeCount());
        assertEquals(List.of(PersonVerificationFailureCategory.LATE_CALLBACK),
            fixture.mapperFixture.audits().stream()
                .map(org.namewta.profile.person.domain.verification.PersonVerificationSecurityAudit::category)
                .toList());
    }

    @Test
    void rejectsCallbackWhenApplicationProviderNoLongerMatches() {
        Fixture fixture = fixture("WAITING");
        fixture.mapperFixture.setApplication(new PersonApplicationVerificationState(
            41L, 501L, "other-provider", "WAITING"));

        PersonVerificationException failure = assertThrows(PersonVerificationException.class,
            () -> fixture.coordinator.handleCallback(
                "test-provider", fixture.callback("approved", NOW), NOW));

        assertEquals(PersonVerificationFailureCategory.PROVIDER_MISMATCH, failure.category());
        assertEquals(0, fixture.mapperFixture.completeCount());
        assertEquals(List.of(PersonVerificationFailureCategory.PROVIDER_MISMATCH),
            fixture.mapperFixture.audits().stream()
                .map(org.namewta.profile.person.domain.verification.PersonVerificationSecurityAudit::category)
                .toList());
    }

    private Fixture fixture(String applicationStatus) {
        PersonDeterministicTestProvider provider = new PersonDeterministicTestProvider("person-test-secret");
        PersonVerificationProviderProperties properties = new PersonVerificationProviderProperties();
        properties.setEnabledProviders(Set.of("test-provider"));
        PersonVerificationMapperFixture mapperFixture = new PersonVerificationMapperFixture(
            new PersonApplicationVerificationState(41L, 501L, "test-provider", applicationStatus));
        mapperFixture.addAttempt(new PersonVerificationAttempt(
            91L, 41L, 501L, "test-provider", "person-41-1", "fingerprint", null, 1,
            PersonProviderAttemptStatus.PENDING, null, null, null, null));
        PersonVerificationSecurityAuditRecorder recorder =
            new PersonVerificationSecurityAuditRecorder(new PersonVerificationAttemptDao(mapperFixture.mapper()));
        PersonVerificationAttemptService coordinator = new PersonVerificationAttemptService(
            new PersonVerificationProviderRegistry(List.of(provider), properties),
            new PersonVerificationAttemptDao(mapperFixture.mapper()),
            mapperFixture.evidenceCodec(), recorder);
        return new Fixture(provider, mapperFixture, coordinator);
    }

    private record Fixture(
        PersonDeterministicTestProvider provider,
        PersonVerificationMapperFixture mapperFixture,
        PersonVerificationAttemptService coordinator
    ) {
        PersonProviderCallbackEnvelope callback(String payload, Instant timestamp) {
            String requestId = "person-41-1";
            return new PersonProviderCallbackEnvelope(
                requestId,
                timestamp.getEpochSecond(),
                payload,
                provider.sign(requestId, timestamp.getEpochSecond(), payload));
        }
    }

}
