package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.service.EnterpriseVerificationAttemptService;

import org.namewta.profile.enterprise.adapter.provider.EnterpriseVerificationProviderRegistry;

import org.namewta.profile.enterprise.domain.application.EnterpriseDocumentTypeRule;
import org.namewta.profile.enterprise.domain.application.EnterpriseApplication;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationVo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationSaveBo;
import org.namewta.profile.enterprise.domain.application.EnterpriseDraftUpdate;
import org.namewta.profile.enterprise.domain.application.EnterpriseIdentityFields;
import org.namewta.profile.enterprise.domain.bo.EnterpriseApplicationProbeBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseApplicationProbeVo;
import org.namewta.profile.enterprise.domain.application.EnterprisePublication;
import org.namewta.profile.enterprise.domain.application.EnterpriseSubmission;
import org.namewta.profile.enterprise.port.gateway.EnterpriseWorkflowGateway;
import org.namewta.profile.enterprise.mapper.EnterpriseApplicationMapper;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.event.ProcessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class EnterpriseApplicationServiceTest {

    private final EnterpriseApplicationMapper mapper = mock(EnterpriseApplicationMapper.class);
    private final ProfileMaterialPort materials = mock(ProfileMaterialPort.class);
    private final EnterpriseVerificationProviderRegistry providers = mock(EnterpriseVerificationProviderRegistry.class);
    private final EnterpriseVerificationAttemptService attempts = mock(EnterpriseVerificationAttemptService.class);
    private final EnterpriseWorkflowGateway workflow = mock(EnterpriseWorkflowGateway.class);
    private final ConfigService config = mock(ConfigService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);
    private EnterpriseApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new EnterpriseApplicationServiceImpl(mapper, JsonMapper.builder().build(), materials,
            providers, attempts, workflow, config, clock));
    }

    @Test
    void savesCompleteEnterpriseDraftWithoutPrefillingAnExistingProfile() {
        when(config.getConfigValue("profile.enterprise.provider.default")).thenReturn("manual");
        doReturn(Optional.empty()).when(service).findOpenByUserId(101L);
        doReturn(null).when(service).findEffectiveProfileIdByUser(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        doReturn(9201L).when(service).findActiveProfileIdByIdentity("91310000ABCDEF123Y");
        doReturn(application(9001L, "DRAFT", 0, 0, true))
            .when(service).saveDraft(eq(101L), eq("manual"), any());

        EnterpriseApplicationVo result = service.save(101L, command(true, 0));

        assertThat(result.enterpriseName()).isEqualTo("示例企业");
        org.mockito.ArgumentCaptor<EnterpriseDraftUpdate> update =
            org.mockito.ArgumentCaptor.forClass(EnterpriseDraftUpdate.class);
        verify(service).saveDraft(eq(101L), eq("manual"), update.capture());
        assertThat(update.getValue().targetProfileId()).isEqualTo(9201L);
        assertThat(update.getValue().fields().email()).isEqualTo("ops@example.com");
    }

    @Test
    void probeReturnsOnlyTheMinimalStatus() {
        doReturn("BOUND").when(service).probeStatus("91310000ABCDEF123Y");

        EnterpriseApplicationProbeVo result = service.probe(new EnterpriseApplicationProbeBo(" 91310000abcdef123y "));

        assertThat(result).isEqualTo(new EnterpriseApplicationProbeVo("BOUND"));
    }

    @Test
    void requiresAuthorizationLetterOnlyForANonLegalRepresentativeHandler() {
        EnterpriseApplication application = application(9001L, "DRAFT", 0, 0, false);
        EnterpriseSubmission submission = new EnterpriseSubmission(9101L, 9001L, 1, 101L,
            application.fields(), "manual", clock.instant());
        doReturn(application).when(service).lockOpenByUserId(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        doReturn(submission).when(service).insertSubmission(eq(application), any(), eq(1), any());
        doReturn(application(9001L, "WAITING", 1, 1, false))
            .when(service).markWaiting(9001L, 1, 0, clock.instant());
        doNothing().when(service).requireSubmissionAllowed(anyLong(), any(), any());

        service.submit(101L, 0);

        ProfileMaterialPort.MaterialOwnerKey working = new ProfileMaterialPort.MaterialOwnerKey(
            ProfileType.ENTERPRISE, ProfileMaterialPort.MaterialOwnerType.WORKING, 9001L);
        verify(materials).validateRequired(working, "*",
            Set.of("ALWAYS", "HANDLER_NOT_LEGAL_REPRESENTATIVE"));
        verify(workflow).start(9001L, 9101L, 1);
    }

    @Test
    void legalRepresentativeHandlerUsesTheBaseMaterialRules() {
        EnterpriseApplication application = application(9001L, "DRAFT", 0, 0, true);
        EnterpriseSubmission submission = new EnterpriseSubmission(9101L, 9001L, 1, 101L,
            application.fields(), "manual", clock.instant());
        doReturn(application).when(service).lockOpenByUserId(101L);
        doReturn(Optional.of(documentType())).when(service).findDocumentType("CN_RESIDENT_ID");
        doReturn(submission).when(service).insertSubmission(eq(application), any(), eq(1), any());
        doReturn(application(9001L, "WAITING", 1, 1, true))
            .when(service).markWaiting(anyLong(), anyInt(), anyInt(), any());
        doNothing().when(service).requireSubmissionAllowed(anyLong(), any(), any());

        service.submit(101L, 0);

        verify(materials).validateRequired(any(), eq("*"), eq(Set.of("ALWAYS")));
    }

    @Test
    void finishesOnlyTheCurrentSnapshotAndPublishesVersionMaterials() {
        EnterpriseApplication waiting = application(9001L, "WAITING", 2, 3, true);
        when(config.getConfigValue("profile.enterprise.flowCode")).thenReturn("profile_enterprise_verification");
        doReturn(waiting).when(service).lockById(9001L);
        doReturn(new EnterpriseSubmission(
            9101L, 9001L, 3, 101L, waiting.fields(), "manual", clock.instant()))
            .when(service).requireSubmission(9001L, 3);
        doReturn(new EnterprisePublication(9201L, 9301L, 9401L, false))
            .when(service).publishApproved(9001L, 3, clock.instant());

        service.handleProcessEvent(event("finish", 3));
        service.handleProcessEvent(event("finish", 2));

        verify(service).publishApproved(9001L, 3, clock.instant());
        verify(materials).snapshotImmutable(
            new ProfileMaterialPort.MaterialOwnerKey(ProfileType.ENTERPRISE,
                ProfileMaterialPort.MaterialOwnerType.SUBMISSION, 9101L),
            new ProfileMaterialPort.MaterialOwnerKey(ProfileType.ENTERPRISE,
                ProfileMaterialPort.MaterialOwnerType.VERSION, 9301L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BACK", "CANCEL", "INVALID", "TERMINATION"})
    void recordsEveryNonApprovalTerminalWithoutPublishing(String status) {
        when(config.getConfigValue("profile.enterprise.flowCode")).thenReturn("profile_enterprise_verification");
        doReturn(application(9001L, "WAITING", 2, 1, true)).when(service).lockById(9001L);
        doNothing().when(service).updateWorkflowStatus(anyLong(), anyInt(), any(), anyInt(), any());

        service.handleProcessEvent(event(status.toLowerCase(java.util.Locale.ROOT), 1));

        verify(service).updateWorkflowStatus(9001L, 1, status, 2, clock.instant());
        verify(service, never()).publishApproved(anyLong(), anyInt(), any());
    }

    @Test
    void workflowReviewRejectInvalidatesTheSnapshotWithoutPublishing() {
        when(config.getConfigValue("profile.enterprise.flowCode")).thenReturn("profile_enterprise_verification");
        doReturn(application(9001L, "WAITING", 2, 1, true)).when(service).lockById(9001L);
        doNothing().when(service).updateWorkflowStatus(anyLong(), anyInt(), any(), anyInt(), any());
        ProcessEvent rejected = event("finish", 1);
        rejected.setParams(Map.of("snapshotVersion", 1, "profileDecision", "REJECT"));

        service.handleProcessEvent(rejected);

        verify(service).updateWorkflowStatus(9001L, 1, "INVALID", 2, clock.instant());
        verify(service, never()).publishApproved(anyLong(), anyInt(), any());
    }

    @Test
    void rejectsExpiredBusinessTermsAndInvalidCreditCodes() {
        when(config.getConfigValue("profile.enterprise.provider.default")).thenReturn("manual");
        doReturn(Optional.empty()).when(service).findOpenByUserId(101L);
        EnterpriseApplicationSaveBo invalid = new EnterpriseApplicationSaveBo("企业", "bad", "COMPANY", "法人",
            "CN_RESIDENT_ID", "110101199001011234", true, LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 1, 1), LocalDate.of(2025, 1, 1), "地址", "范围",
            null, null, null, null, null, null, 0);

        assertThatThrownBy(() -> service.save(101L, invalid))
            .hasMessage("ENTERPRISE_CREDIT_CODE_INVALID");
        verify(service, never()).saveDraft(anyLong(), any(), any());
    }

    private EnterpriseApplicationSaveBo command(boolean legalHandler, int version) {
        return new EnterpriseApplicationSaveBo("示例企业", "91310000abcdef123y", "COMPANY", "张法",
            "CN_RESIDENT_ID", "110101199001011234", legalHandler, LocalDate.of(2010, 1, 1),
            LocalDate.of(2010, 1, 1), LocalDate.of(2035, 1, 1), "上海市示例路 1 号", "软件服务",
            "李联", "13800138000", "OPS@EXAMPLE.COM", new BigDecimal("1000000.00"),
            "SOFTWARE", "https://example.com", version);
    }

    private EnterpriseApplication application(long id, String status, int version, int submissionSeq,
                                              boolean legalHandler) {
        EnterpriseIdentityFields fields = EnterpriseIdentityFields.normalize(command(legalHandler, version));
        return new EnterpriseApplication(id, 101L, null, status, fields, "manual",
            submissionSeq, 0, version, null, null);
    }

    private EnterpriseDocumentTypeRule documentType() {
        return new EnterpriseDocumentTypeRule("CN_RESIDENT_ID", "^[0-9]{17}[0-9Xx]$", true);
    }

    private ProcessEvent event(String status, int snapshotVersion) {
        ProcessEvent event = new ProcessEvent();
        event.setFlowCode("profile_enterprise_verification");
        event.setBusinessId("9001");
        event.setStatus(status);
        event.setParams(Map.of("snapshotVersion", snapshotVersion));
        return event;
    }
}
