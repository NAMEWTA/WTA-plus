package org.namewta.profile.person.adapter.security;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.person.domain.material.MaterialOwner;
import org.namewta.system.api.ConfigService;
import org.namewta.workflow.api.WorkflowTaskReviewService;
import org.namewta.workflow.api.domain.WorkflowTaskReviewContext;
import org.springframework.beans.factory.ObjectProvider;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class SaTokenProfileMaterialAccessPolicyTest {
    private final WorkflowTaskReviewService reviews = mock(WorkflowTaskReviewService.class);
    private final ConfigService config = mock(ConfigService.class);
    private final ObjectProvider<WorkflowTaskReviewService> provider = provider();
    private final SaTokenProfileMaterialAccessPolicy policy = new SaTokenProfileMaterialAccessPolicy(provider, config);

    @ParameterizedTest
    @EnumSource(ProfileType.class)
    void taskReviewPermissionDoesNotGrantGlobalReadOrWrite(ProfileType type) {
        var owner = owner(type, MaterialOwnerType.SUBMISSION, 601L);
        try (var token = mockStatic(StpUtil.class); var login = mockStatic(LoginHelper.class)) {
            token.when(() -> StpUtil.hasPermission(anyString())).thenAnswer(
                invocation -> invocation.getArgument(0, String.class).endsWith(":task-review"));
            login.when(LoginHelper::getUserId).thenReturn(999L);
            assertThatThrownBy(() -> policy.requireRead(owner)).hasMessage("MATERIAL_ACCESS_DENIED");
            assertThatThrownBy(() -> policy.requireWrite(owner)).hasMessage("MATERIAL_ACCESS_DENIED");
            assertThatThrownBy(() -> policy.requireAttach(owner)).hasMessage("MATERIAL_ACCESS_DENIED");
        }
        verifyNoInteractions(reviews);
    }

    @ParameterizedTest
    @EnumSource(ProfileType.class)
    void authorizedCurrentOrHistoricalTaskCanOnlyReadItsBoundSubmission(ProfileType type) {
        String kind = type.name().toLowerCase(Locale.ROOT);
        when(provider.getIfAvailable()).thenReturn(reviews);
        when(config.getConfigValue("profile." + kind + ".flowCode")).thenReturn("profile_" + kind);
        when(reviews.readTaskContext(701L)).thenReturn(context("profile_" + kind, 601L));
        try (var token = mockStatic(StpUtil.class)) {
            token.when(() -> StpUtil.hasPermission("profile:" + kind + ":task-review")).thenReturn(true);
            policy.requireTaskRead(owner(type, MaterialOwnerType.SUBMISSION, 601L), 701L);
            assertThatThrownBy(() -> policy.requireTaskRead(owner(type, MaterialOwnerType.SUBMISSION, 602L), 701L))
                .hasMessage("MATERIAL_ACCESS_DENIED");
            assertThatThrownBy(() -> policy.requireTaskRead(owner(type, MaterialOwnerType.WORKING, 601L), 701L))
                .hasMessage("MATERIAL_ACCESS_DENIED");
            assertThatThrownBy(() -> policy.requireTaskRead(owner(type, MaterialOwnerType.VERSION, 601L), 701L))
                .hasMessage("MATERIAL_ACCESS_DENIED");
        }
        verify(reviews, never()).requireTaskContext(anyLong());
        verify(reviews, never()).approve(anyLong(), anyString());
    }

    @Test
    void differentBusinessFlowOrMissingPermissionCannotReadMaterials() {
        when(provider.getIfAvailable()).thenReturn(reviews);
        when(config.getConfigValue("profile.person.flowCode")).thenReturn("profile_person");
        when(reviews.readTaskContext(701L)).thenReturn(context("profile_enterprise", 601L));
        var owner = owner(ProfileType.PERSON, MaterialOwnerType.SUBMISSION, 601L);
        try (var token = mockStatic(StpUtil.class)) {
            assertThatThrownBy(() -> policy.requireTaskRead(owner, 701L)).hasMessage("MATERIAL_ACCESS_DENIED");
            verifyNoInteractions(reviews);
            token.when(() -> StpUtil.hasPermission("profile:person:task-review")).thenReturn(true);
            assertThatThrownBy(() -> policy.requireTaskRead(owner, 701L)).hasMessage("MATERIAL_ACCESS_DENIED");
        }
    }

    @Test
    void coreBundleWithoutWorkflowDoesNotFallbackToGlobalRead() {
        try (var token = mockStatic(StpUtil.class)) {
            token.when(() -> StpUtil.hasPermission("profile:person:task-review")).thenReturn(true);
            assertThatThrownBy(() -> new SaTokenProfileMaterialAccessPolicy().requireTaskRead(
                owner(ProfileType.PERSON, MaterialOwnerType.SUBMISSION, 601L), 701L))
                .hasMessage("MATERIAL_ACCESS_DENIED");
        }
    }

    private MaterialOwner owner(ProfileType type, MaterialOwnerType ownerType, long id) {
        return new MaterialOwner(new MaterialOwnerKey(type, ownerType, id), 101L);
    }

    private WorkflowTaskReviewContext context(String flowCode, Long submissionId) {
        return new WorkflowTaskReviewContext(701L, 401L, "501", flowCode, 20L, 2, submissionId);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<WorkflowTaskReviewService> provider() {
        return mock(ObjectProvider.class);
    }
}
