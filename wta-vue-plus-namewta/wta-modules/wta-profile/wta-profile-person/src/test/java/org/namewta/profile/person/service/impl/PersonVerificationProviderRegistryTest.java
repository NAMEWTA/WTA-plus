package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.adapter.provider.PersonVerificationProviderRegistry;

import org.namewta.profile.person.config.PersonVerificationProviderProperties;

import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonProviderStartCommand;
import org.namewta.profile.person.domain.verification.PersonProviderStartResult;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.namewta.profile.person.domain.verification.PersonVerifiedCallback;
import org.namewta.profile.person.port.provider.PersonVerificationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class PersonVerificationProviderRegistryTest {

    @Test
    void resolvesOnlyTheConfiguredProviderWithoutFallback() {
        PersonVerificationProvider manual = provider("manual");
        PersonVerificationProvider test = provider("test-provider");
        PersonVerificationProviderProperties properties = new PersonVerificationProviderProperties();
        properties.setEnabledProviders(Set.of("manual"));
        PersonVerificationProviderRegistry registry = new PersonVerificationProviderRegistry(
            List.of(manual, test), properties);

        assertSame(manual, registry.requireEnabled("manual"));
        assertEquals(PersonVerificationFailureCategory.DISABLED_PROVIDER,
            assertThrows(PersonVerificationException.class,
                () -> registry.requireEnabled("test-provider")).category());
        assertEquals(PersonVerificationFailureCategory.UNKNOWN_PROVIDER,
            assertThrows(PersonVerificationException.class,
                () -> registry.requireEnabled("missing")).category());
    }

    @Test
    void rejectsDuplicateProviderCodes() {
        PersonVerificationProviderProperties properties = new PersonVerificationProviderProperties();

        PersonVerificationException failure = assertThrows(PersonVerificationException.class,
            () -> new PersonVerificationProviderRegistry(List.of(provider("manual"), provider("manual")), properties));

        assertEquals(PersonVerificationFailureCategory.DUPLICATE_PROVIDER, failure.category());
    }

    private PersonVerificationProvider provider(String providerCode) {
        return new PersonVerificationProvider() {
            @Override
            public String providerCode() {
                return providerCode;
            }

            @Override
            public PersonProviderStartResult start(PersonProviderStartCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PersonVerifiedCallback authenticate(PersonProviderCallbackEnvelope callback, Instant receivedAt) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
