package org.namewta.profile.person.service;

import org.namewta.profile.person.port.verification.PersonVerificationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.profile.person.controller.self.PersonApplicationController;
import org.namewta.profile.person.dao.PersonApplicationDao;
import org.namewta.profile.person.domain.model.read.PersonApplicationRow;
import org.namewta.profile.person.domain.model.read.PersonVersionRow;
import org.namewta.profile.person.port.gateway.PersonWorkflowGateway;
import org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort;
import org.namewta.profile.person.usecase.impl.PersonApplicationUseCaseImpl;
import org.namewta.system.api.ConfigService;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class PersonSelfSummaryTest {
    private final PersonApplicationDao dao = mock(PersonApplicationDao.class);
    private final PersonApplicationService service = new PersonApplicationService(dao,
        mock(ProfileMaterialPort.class), mock(PersonVerificationProviderRegistryPort.class),
        mock(PersonVerificationService.class), mock(PersonWorkflowGateway.class), mock(ConfigService.class));
    private final PersonApplicationController controller = new PersonApplicationController(new PersonApplicationUseCaseImpl(service));

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
            assertThat(result.certifiedProfile().identity().fullName()).isEqualTo("本人实名");
        assertThat(result.certifiedProfile().identity().documentNumber()).isEqualTo("110101199001011234");
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
        var current = new PersonApplicationRow();
        current.setPersonApplicationId(501L);
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
        assertThatThrownBy(() -> service.summary(0)).hasMessage("PERSON_USER_INVALID");
        verifyNoInteractions(dao);
    }

    private PersonVersionRow certified() {
        var row = new PersonVersionRow();
        row.setPersonProfileId(901L);
        row.setPublishedTime(Instant.parse("2026-09-01T12:00:00Z"));
        row.setFullName("本人实名"); row.setDocumentNumber("110101199001011234");
        return row;
    }
}
