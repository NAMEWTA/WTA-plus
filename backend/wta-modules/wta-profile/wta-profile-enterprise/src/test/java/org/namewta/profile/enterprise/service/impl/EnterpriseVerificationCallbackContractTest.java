package org.namewta.profile.enterprise.service.impl;


import org.namewta.profile.enterprise.adapter.codec.EnterpriseVerificationEvidenceCodec;

import org.namewta.profile.enterprise.service.EnterpriseVerificationAttemptService;

import org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry;

import org.namewta.profile.enterprise.config.EnterpriseVerificationProviderProperties;
import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationCallbackOutcome;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationFailureCategory;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationAttemptRow;
import org.namewta.profile.enterprise.mapper.EnterpriseVerificationAttemptMapper;
import org.namewta.profile.enterprise.dao.EnterpriseVerificationAttemptDao;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class EnterpriseVerificationCallbackContractTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");

    @Test
    void authenticatesAndHandlesIdempotentConflictingAndLateCallbacksWithoutPublishing() {
        Fixture fixture = fixture("WAITING", "test-provider");
        EnterpriseProviderCallbackEnvelope accepted = fixture.callback("approved", NOW);

        assertThat(fixture.coordinator.handleCallback("test-provider", accepted, NOW))
            .isEqualTo(EnterpriseVerificationCallbackOutcome.ACCEPTED);
        assertThat(fixture.coordinator.handleCallback("test-provider", accepted, NOW.plusSeconds(1)))
            .isEqualTo(EnterpriseVerificationCallbackOutcome.IDEMPOTENT);
        assertThatThrownBy(() -> fixture.coordinator.handleCallback(
            "test-provider", fixture.callback("rejected", NOW.plusSeconds(2)), NOW.plusSeconds(2)))
            .isInstanceOf(EnterpriseVerificationException.class)
            .extracting(failure -> ((EnterpriseVerificationException) failure).category())
            .isEqualTo(EnterpriseVerificationFailureCategory.CONFLICTING_CALLBACK);

        assertThat(fixture.completeCount).hasValue(1);
        assertThat(fixture.audits).containsExactly(EnterpriseVerificationFailureCategory.CONFLICTING_CALLBACK);
        assertThat(fixture.application.getStatus()).isEqualTo("WAITING");

        Fixture terminal = fixture("FINISH", "test-provider");
        assertThat(terminal.coordinator.handleCallback(
            "test-provider", terminal.callback("approved", NOW), NOW))
            .isEqualTo(EnterpriseVerificationCallbackOutcome.LATE_IGNORED);
        assertThat(terminal.completeCount).hasValue(0);
        assertThat(terminal.application.getStatus()).isEqualTo("FINISH");
    }

    @Test
    void auditsForgedAndExpiredCallbacks() {
        Fixture fixture = fixture("WAITING", "test-provider");

        assertThatThrownBy(() -> fixture.coordinator.handleCallback("test-provider",
            new EnterpriseProviderCallbackEnvelope(
                "enterprise-71-1", NOW.getEpochSecond(), "approved", "00"), NOW))
            .isInstanceOf(EnterpriseVerificationException.class)
            .extracting(failure -> ((EnterpriseVerificationException) failure).category())
            .isEqualTo(EnterpriseVerificationFailureCategory.INVALID_SIGNATURE);
        assertThatThrownBy(() -> fixture.coordinator.handleCallback(
            "test-provider", fixture.callback("approved", NOW.minusSeconds(301)), NOW))
            .isInstanceOf(EnterpriseVerificationException.class)
            .extracting(failure -> ((EnterpriseVerificationException) failure).category())
            .isEqualTo(EnterpriseVerificationFailureCategory.EXPIRED_CALLBACK);

        assertThat(fixture.completeCount).hasValue(0);
        assertThat(fixture.audits).containsExactly(
            EnterpriseVerificationFailureCategory.INVALID_SIGNATURE,
            EnterpriseVerificationFailureCategory.EXPIRED_CALLBACK);
    }

    @Test
    void rejectsCallbackWhenApplicationProviderNoLongerMatches() {
        Fixture fixture = fixture("WAITING", "other-provider");

        assertThatThrownBy(() -> fixture.coordinator.handleCallback(
            "test-provider", fixture.callback("approved", NOW), NOW))
            .isInstanceOf(EnterpriseVerificationException.class)
            .extracting(failure -> ((EnterpriseVerificationException) failure).category())
            .isEqualTo(EnterpriseVerificationFailureCategory.PROVIDER_MISMATCH);
        assertThat(fixture.completeCount).hasValue(0);
        assertThat(fixture.audits).containsExactly(EnterpriseVerificationFailureCategory.PROVIDER_MISMATCH);
    }

    private Fixture fixture(String applicationStatus, String applicationProviderCode) {
        return new Fixture(applicationStatus, applicationProviderCode);
    }

    private static final class Fixture {
        private final EnterpriseDeterministicTestProvider provider =
            new EnterpriseDeterministicTestProvider("enterprise-test-secret");
        private final EnterpriseVerificationApplicationRow application = new EnterpriseVerificationApplicationRow();
        private final AtomicReference<EnterpriseVerificationAttemptRow> attempt =
            new AtomicReference<>(attempt());
        private final AtomicInteger completeCount = new AtomicInteger();
        private final List<EnterpriseVerificationFailureCategory> audits = new ArrayList<>();
        private final EnterpriseVerificationAttemptService coordinator;

        private Fixture(String applicationStatus, String applicationProviderCode) {
            EnterpriseVerificationAttemptMapper mapper = mock(EnterpriseVerificationAttemptMapper.class);
            application.setApplicationId(71L);
            application.setSubmissionId(801L);
            application.setProviderCode(applicationProviderCode);
            application.setStatus(applicationStatus);
            when(mapper.lockApplication(71L)).thenReturn(application);
            when(mapper.lockByProviderRequest("test-provider", "enterprise-71-1"))
                .thenAnswer(ignored -> attempt.get());
            when(mapper.completeAttempt(any(Long.class), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    EnterpriseVerificationAttemptRow current = attempt.get();
                    current.setStatus(invocation.getArgument(1));
                    current.setNormalizedResultJson(invocation.getArgument(2));
                    current.setProviderEvidenceJson(invocation.getArgument(3));
                    current.setErrorCode(invocation.getArgument(4));
                    current.setCompletedTime(invocation.getArgument(5));
                    completeCount.incrementAndGet();
                    return 1;
                });
            when(mapper.insertSecurityAudit(any(Long.class), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    audits.add(EnterpriseVerificationFailureCategory.valueOf(invocation.getArgument(3)));
                    return 1;
                });
            EnterpriseVerificationProviderProperties properties = new EnterpriseVerificationProviderProperties();
            properties.setEnabledProviders(Set.of("test-provider"));
            EnterpriseVerificationEvidenceCodec codec =
                new EnterpriseVerificationEvidenceCodec();
            coordinator = new EnterpriseVerificationAttemptService(
                new EnterpriseVerificationProviderRegistry(List.of(provider), properties),
                new EnterpriseVerificationAttemptDao(mapper), codec);
        }

        private EnterpriseProviderCallbackEnvelope callback(String payload, Instant timestamp) {
            String requestId = "enterprise-71-1";
            return new EnterpriseProviderCallbackEnvelope(
                requestId,
                timestamp.getEpochSecond(),
                payload,
                provider.sign(requestId, timestamp.getEpochSecond(), payload));
        }

        private static EnterpriseVerificationAttemptRow attempt() {
            EnterpriseVerificationAttemptRow row = new EnterpriseVerificationAttemptRow();
            row.setVerificationAttemptId(191L);
            row.setApplicationId(71L);
            row.setSubmissionId(801L);
            row.setProviderCode("test-provider");
            row.setProviderRequestId("enterprise-71-1");
            row.setRequestFingerprint("fingerprint");
            row.setAttemptNo(1);
            row.setStatus("PENDING");
            return row;
        }
    }
}
