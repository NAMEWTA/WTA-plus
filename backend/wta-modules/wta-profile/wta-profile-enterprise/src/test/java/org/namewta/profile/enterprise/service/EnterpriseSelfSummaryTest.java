package org.namewta.profile.enterprise.service;

import org.namewta.profile.enterprise.port.verification.EnterpriseVerificationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.enterprise.controller.self.EnterpriseApplicationController;
import org.namewta.profile.enterprise.dao.EnterpriseApplicationDao;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseApplicationRow;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseVersionRow;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;
import org.namewta.profile.enterprise.port.provider.EnterpriseVerificationProviderRegistryPort;
import org.namewta.profile.enterprise.usecase.impl.EnterpriseApplicationUseCaseImpl;
import org.namewta.system.api.ConfigService;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class EnterpriseSelfSummaryTest {
    private final EnterpriseApplicationDao dao = mock(EnterpriseApplicationDao.class);
    private final EnterpriseApplicationService service = new EnterpriseApplicationService(dao,
        mock(ProfileMaterialPort.class), mock(EnterpriseVerificationProviderRegistryPort.class),
        mock(EnterpriseVerificationService.class), mock(EnterpriseWorkflowGateway.class), mock(ConfigService.class));
    private final EnterpriseApplicationController controller = new EnterpriseApplicationController(new EnterpriseApplicationUseCaseImpl(service));

    @Test
    void returnsOnlyTheLoggedInUsersEffectiveIdentityWithoutMakingItAnEditableApplication() {
        var row = certified();
        when(dao.selectSelfVersion(101L)).thenReturn(row);
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(101L);
            var result = controller.summary().getData();
            assertThat(result.status()).isEqualTo("VERIFIED");
            assertThat(result.currentApplication()).isNull();
            assertThat(result.returnReason()).isNull();
            assertThat(result.certifiedProfile().profileId()).isEqualTo(901L);
            assertThat(result.certifiedProfile().verifiedAt()).isEqualTo(Instant.parse("2026-09-01T12:00:00Z"));
            assertThat(result.certifiedProfile().identity().enterpriseName()).isEqualTo("本人企业");
        assertThat(result.certifiedProfile().identity().unifiedCreditCode()).isEqualTo("911100001234567897");
        }
        verify(dao).selectSelfVersion(101L);
        verify(dao, never()).selectReturnReason(anyLong());
    }

    @Test
    void unverifiedUserHasNoInventedIdentityOrEditableApplication() {
        var result = service.summary(202L);
        assertThat(result.status()).isEqualTo("UNVERIFIED");
        assertThat(result.currentApplication()).isNull();
        assertThat(result.certifiedProfile()).isNull();
        assertThat(result.returnReason()).isNull();
        verify(dao).selectSelfVersion(202L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "WAITING", "CANCEL", "BACK"})
    void openApplicationPreservesAnExistingCertificationAndOnlyBackIncludesItsReason(String status) {
        var current = new EnterpriseApplicationRow();
        current.setEnterpriseApplicationId(501L);
        current.setApplicantUserId(101L);
        current.setStatus(status);
        current.setSubmissionSeq(2);
        current.setVersion(3);
        when(dao.selectOpenByUserId(101L)).thenReturn(current);
        when(dao.selectSelfVersion(101L)).thenReturn(certified());
        when(dao.selectReturnReason(101L)).thenReturn("请补齐有效证件");
        var result = service.summary(101L);
        assertThat(result.status()).isEqualTo(status);
        assertThat(result.currentApplication().status()).isEqualTo(status);
        assertThat(result.certifiedProfile().profileId()).isEqualTo(901L);
        if ("BACK".equals(status)) {
            assertThat(result.returnReason()).isEqualTo("请补齐有效证件");
        } else {
            assertThat(result.returnReason()).isNull();
            verify(dao, never()).selectReturnReason(anyLong());
        }
    }

    @Test
    void missingAccountFailsBeforeAnyProfileRead() {
        assertThatThrownBy(() -> service.summary(0)).hasMessage("ENTERPRISE_USER_INVALID");
        verifyNoInteractions(dao);
    }

    private EnterpriseVersionRow certified() {
        var row = new EnterpriseVersionRow();
        row.setEnterpriseProfileId(901L);
        row.setPublishedTime(Instant.parse("2026-09-01T12:00:00Z"));
        row.setEnterpriseName("本人企业"); row.setUnifiedCreditCode("911100001234567897");
        return row;
    }
}
