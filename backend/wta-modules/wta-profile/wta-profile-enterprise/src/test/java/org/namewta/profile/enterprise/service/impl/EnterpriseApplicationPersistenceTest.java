package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.domain.application.EnterprisePublication;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseBindingRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseProfileRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseSubmissionRow;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class EnterpriseApplicationPersistenceTest {

    private final EnterpriseApplicationMapper mapper = mock(EnterpriseApplicationMapper.class);
    private final EnterpriseApplicationServiceImpl service = new EnterpriseApplicationServiceImpl(
        mapper, null, null, null, null, null, Clock.systemUTC());

    @Test
    void ordinaryApplicationCannotReplaceAnyEffectiveResponsibleAccount() {
        when(mapper.lockEffectiveBindingByUser(101L)).thenReturn(binding(9201L, 101L, "ACTIVE"));

        assertThatThrownBy(() -> service.requireSubmissionAllowed(101L, 9201L, "91310000ABCDEF123Y"))
            .hasMessage("ENTERPRISE_ACCOUNT_ALREADY_RESPONSIBLE");
    }

    @Test
    void ordinaryApplicationCannotReplaceTheExistingEnterpriseResponsibleAccount() {
        EnterpriseProfileRow profile = profile(9201L, 0);
        when(mapper.lockActiveProfileByIdentity("91310000ABCDEF123Y")).thenReturn(profile);
        when(mapper.lockEffectiveBindingByProfile(9201L)).thenReturn(binding(9201L, 202L, "SUSPENDED"));

        assertThatThrownBy(() -> service.requireSubmissionAllowed(101L, 9201L, "91310000ABCDEF123Y"))
            .hasMessage("ENTERPRISE_RESPONSIBLE_ALREADY_BOUND");
    }

    @Test
    void publishesAnUnboundEnterpriseVersionAndExactlyOneResponsibleBinding() {
        EnterpriseApplicationRow application = application();
        EnterpriseSubmissionRow submission = submission();
        EnterpriseProfileRow profile = profile(9201L, 4);
        when(mapper.lockApplicationById(9001L)).thenReturn(application);
        when(mapper.selectSubmission(9001L, 1)).thenReturn(submission);
        when(mapper.lockActiveProfileByIdentity("91310000ABCDEF123Y")).thenReturn(profile);
        when(mapper.insertVersion(any())).thenReturn(1);
        when(mapper.updateProfile(any())).thenReturn(1);
        when(mapper.insertBinding(any())).thenReturn(1);
        when(mapper.insertBindingEvent(any())).thenReturn(1);
        when(mapper.finishApplication(anyLong(), anyInt(), anyInt(), anyInt(), any())).thenReturn(1);

        EnterprisePublication publication =
            service.publishApproved(9001L, 1, Instant.parse("2026-09-01T12:00:00Z"));

        assertThat(publication.enterpriseProfileId()).isEqualTo(9201L);
        ArgumentCaptor<EnterpriseBindingRow> responsible = ArgumentCaptor.forClass(EnterpriseBindingRow.class);
        verify(mapper).insertBinding(responsible.capture());
        assertThat(responsible.getValue().getUserId()).isEqualTo(101L);
        assertThat(responsible.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(responsible.getValue().getEnterpriseProfileId()).isEqualTo(9201L);
    }

    private EnterpriseApplicationRow application() {
        EnterpriseApplicationRow row = new EnterpriseApplicationRow();
        row.setEnterpriseApplicationId(9001L);
        row.setApplicantUserId(101L);
        row.setTargetProfileId(9201L);
        row.setStatus("WAITING");
        row.setSubmissionSeq(1);
        row.setDecisionVersion(0);
        row.setVersion(1);
        return row;
    }

    private EnterpriseSubmissionRow submission() {
        EnterpriseSubmissionRow row = new EnterpriseSubmissionRow();
        row.setEnterpriseSubmissionId(9101L);
        row.setEnterpriseApplicationId(9001L);
        row.setSubmissionSeq(1);
        row.setApplicantUserId(101L);
        row.setTargetProfileId(9201L);
        row.setEnterpriseName("示例企业");
        row.setUnifiedCreditCode("91310000ABCDEF123Y");
        row.setIdentityKey("91310000ABCDEF123Y");
        row.setEnterpriseType("COMPANY");
        row.setLegalRepresentativeName("张法");
        row.setLegalDocumentTypeCode("CN_RESIDENT_ID");
        row.setLegalDocumentNumber("110101199001011234");
        row.setEstablishedDate(LocalDate.of(2010, 1, 1));
        row.setRegisteredAddress("上海");
        row.setBusinessScope("软件");
        row.setEmail("ops@example.com");
        row.setRegisteredCapital(new BigDecimal("1000000.00"));
        row.setProviderCode("manual");
        return row;
    }

    private EnterpriseProfileRow profile(long profileId, int version) {
        EnterpriseProfileRow row = new EnterpriseProfileRow();
        row.setEnterpriseProfileId(profileId);
        row.setUnifiedCreditCode("91310000ABCDEF123Y");
        row.setStatus("ACTIVE");
        row.setVersion(version);
        return row;
    }

    private EnterpriseBindingRow binding(long profileId, long userId, String status) {
        EnterpriseBindingRow row = new EnterpriseBindingRow();
        row.setEnterpriseBindingId(9401L);
        row.setEnterpriseProfileId(profileId);
        row.setUserId(userId);
        row.setStatus(status);
        row.setBindingVersion(1);
        return row;
    }
}
