package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry;

import org.namewta.profile.enterprise.config.EnterpriseVerificationProviderProperties;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProvider;
import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartCommand;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderStartResult;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationFailureCategory;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerifiedCallback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class EnterpriseVerificationProviderRegistryTest {

    @Test
    void resolvesOnlyTheConfiguredProviderWithoutFallback() {
        EnterpriseVerificationProvider manual = provider("manual");
        EnterpriseVerificationProvider test = provider("test-provider");
        EnterpriseVerificationProviderProperties properties = new EnterpriseVerificationProviderProperties();
        properties.setEnabledProviders(Set.of("manual"));
        EnterpriseVerificationProviderRegistry registry = new EnterpriseVerificationProviderRegistry(
            List.of(manual, test), properties);

        assertSame(manual, registry.requireEnabled("manual"));
        assertEquals(EnterpriseVerificationFailureCategory.DISABLED_PROVIDER,
            assertThrows(EnterpriseVerificationException.class,
                () -> registry.requireEnabled("test-provider")).category());
        assertEquals(EnterpriseVerificationFailureCategory.UNKNOWN_PROVIDER,
            assertThrows(EnterpriseVerificationException.class,
                () -> registry.requireEnabled("missing")).category());
    }

    @Test
    void rejectsDuplicateProviderCodes() {
        EnterpriseVerificationProviderProperties properties = new EnterpriseVerificationProviderProperties();

        EnterpriseVerificationException failure = assertThrows(EnterpriseVerificationException.class,
            () -> new EnterpriseVerificationProviderRegistry(List.of(provider("manual"), provider("manual")), properties));

        assertEquals(EnterpriseVerificationFailureCategory.DUPLICATE_PROVIDER, failure.category());
    }

    private EnterpriseVerificationProvider provider(String providerCode) {
        return new EnterpriseVerificationProvider() {
            @Override
            public String providerCode() {
                return providerCode;
            }

            @Override
            public EnterpriseProviderStartResult start(EnterpriseProviderStartCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public EnterpriseVerifiedCallback authenticate(EnterpriseProviderCallbackEnvelope callback, Instant receivedAt) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
