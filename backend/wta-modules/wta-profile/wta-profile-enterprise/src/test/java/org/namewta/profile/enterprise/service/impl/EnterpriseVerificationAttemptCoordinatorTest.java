package org.namewta.profile.enterprise.service.impl;


import org.namewta.profile.enterprise.adapter.codec.EnterpriseVerificationEvidenceCodec;

import org.namewta.profile.enterprise.service.EnterpriseVerificationAttemptService;

import org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry;
import org.namewta.profile.enterprise.adapter.provider.EnterpriseManualVerificationProvider;

import org.namewta.profile.enterprise.config.EnterpriseVerificationProviderProperties;
import org.namewta.profile.enterprise.domain.exception.EnterpriseVerificationException;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationAttempt;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationFailureCategory;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationStartAttemptCommand;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVerificationAttemptRow;
import org.namewta.profile.enterprise.mapper.EnterpriseVerificationAttemptMapper;
import org.namewta.profile.enterprise.dao.EnterpriseVerificationAttemptDao;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class EnterpriseVerificationAttemptServiceTest {

    @Test
    void explicitRetryAppendsAnAttemptUsingTheProviderFixedOnTheApplication() {
        EnterpriseVerificationAttemptMapper mapper = mock(EnterpriseVerificationAttemptMapper.class);
        when(mapper.lockApplication(71L)).thenReturn(application());
        when(mapper.nextAttemptNo(71L)).thenReturn(1, 2);
        when(mapper.insertAttempt(any())).thenReturn(1);
        EnterpriseVerificationAttemptService coordinator = coordinator(mapper, Set.of("manual"));

        EnterpriseVerificationAttempt first = coordinator.startAttempt(
            new EnterpriseVerificationStartAttemptCommand(71L, 801L, "fingerprint-1"));
        EnterpriseVerificationAttempt second = coordinator.startAttempt(
            new EnterpriseVerificationStartAttemptCommand(71L, 801L, "fingerprint-2"));

        assertThat(first.attemptNo()).isEqualTo(1);
        assertThat(second.attemptNo()).isEqualTo(2);
        ArgumentCaptor<EnterpriseVerificationAttemptRow> rows =
            ArgumentCaptor.forClass(EnterpriseVerificationAttemptRow.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertAttempt(rows.capture());
        assertThat(rows.getAllValues()).extracting(EnterpriseVerificationAttemptRow::getProviderCode)
            .containsExactly("manual", "manual");
    }

    @Test
    void retryRejectsAStaleSubmissionBeforeCallingAnyProvider() {
        EnterpriseVerificationAttemptMapper mapper = mock(EnterpriseVerificationAttemptMapper.class);
        when(mapper.lockApplication(71L)).thenReturn(application());
        EnterpriseVerificationAttemptService coordinator = coordinator(mapper, Set.of());

        assertThatThrownBy(() -> coordinator.startAttempt(
            new EnterpriseVerificationStartAttemptCommand(71L, 800L, "stale-fingerprint")))
            .isInstanceOf(EnterpriseVerificationException.class)
            .extracting(failure -> ((EnterpriseVerificationException) failure).category())
            .isEqualTo(EnterpriseVerificationFailureCategory.STALE_SUBMISSION);
        verify(mapper, never()).insertAttempt(any());
    }

    private EnterpriseVerificationAttemptService coordinator(EnterpriseVerificationAttemptMapper mapper,
                                                                 Set<String> enabledProviders) {
        EnterpriseVerificationProviderProperties properties = new EnterpriseVerificationProviderProperties();
        properties.setEnabledProviders(enabledProviders);
        EnterpriseVerificationEvidenceCodec codec =
            new EnterpriseVerificationEvidenceCodec();
        return new EnterpriseVerificationAttemptService(
            new EnterpriseVerificationProviderRegistry(
                List.of(new EnterpriseManualVerificationProvider()), properties),
            new EnterpriseVerificationAttemptDao(mapper), codec);
    }

    private EnterpriseVerificationApplicationRow application() {
        EnterpriseVerificationApplicationRow row = new EnterpriseVerificationApplicationRow();
        row.setApplicationId(71L);
        row.setSubmissionId(801L);
        row.setProviderCode("manual");
        row.setStatus("WAITING");
        return row;
    }
}
