package org.namewta.profile.person.service.impl;


import org.namewta.profile.person.adapter.provider.PersonManualVerificationProvider;

import org.namewta.profile.person.service.PersonVerificationAttemptService;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;
import org.namewta.profile.person.adapter.provider.PersonManualVerificationProvider;

import org.namewta.profile.person.dao.PersonVerificationAttemptDao;
import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonApplicationVerificationState;
import org.namewta.profile.person.domain.verification.PersonVerificationAttempt;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.profile.person.domain.verification.PersonVerificationStartAttemptCommand;
import org.namewta.profile.person.config.PersonVerificationProviderProperties;
import org.namewta.profile.person.support.PersonVerificationTimeSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class PersonVerificationAttemptServiceTest {

    @Test
    void explicitRetryAppendsAnAttemptUsingTheProviderFixedOnTheApplication() {
        PersonVerificationMapperFixture fixture = new PersonVerificationMapperFixture(
            new PersonApplicationVerificationState(41L, 501L, "manual", "WAITING"));
        PersonVerificationProviderProperties properties = new PersonVerificationProviderProperties();
        properties.setEnabledProviders(Set.of("manual"));
        PersonVerificationAttemptService coordinator = new PersonVerificationAttemptService(
            new PersonVerificationProviderRegistry(List.of(new PersonManualVerificationProvider()), properties),
            new PersonVerificationAttemptDao(fixture.mapper()), fixture.evidenceCodec());

        PersonVerificationAttempt first = coordinator.startAttempt(
            new PersonVerificationStartAttemptCommand(41L, 501L, "fingerprint-1"));
        PersonVerificationAttempt second = coordinator.startAttempt(
            new PersonVerificationStartAttemptCommand(41L, 501L, "fingerprint-2"));

        assertEquals(1, first.attemptNo());
        assertEquals(2, second.attemptNo());
        assertEquals(List.of("manual", "manual"),
            fixture.attempts().stream().map(PersonVerificationAttempt::providerCode).toList());
    }

    @Test
    void retryRejectsAStaleSubmissionBeforeCallingAnyProvider() {
        PersonVerificationMapperFixture fixture = new PersonVerificationMapperFixture(
            new PersonApplicationVerificationState(41L, 501L, "manual", "WAITING"));
        PersonVerificationProviderProperties properties = new PersonVerificationProviderProperties();
        PersonVerificationAttemptService coordinator = new PersonVerificationAttemptService(
            new PersonVerificationProviderRegistry(List.of(new PersonManualVerificationProvider()), properties),
            new PersonVerificationAttemptDao(fixture.mapper()), fixture.evidenceCodec());

        PersonVerificationException failure = assertThrows(PersonVerificationException.class,
            () -> coordinator.startAttempt(
                new PersonVerificationStartAttemptCommand(41L, 500L, "stale-fingerprint")));

        assertEquals(PersonVerificationFailureCategory.STALE_SUBMISSION, failure.category());
        assertEquals(0, fixture.attempts().size());
    }
}
