package org.namewta.common.satoken.profile;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.domain.PersonDisclosure;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.system.api.model.LoginUser;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 认证门禁必须绑定可信会话并共享公共查询语义，查询失败不能被误判为未认证。 */
@Tag("dev")
class ProfileAccessTest {
    private final ProfileService profiles = mock(ProfileService.class);
    private final ProfileDisclosureService disclosures = mock(ProfileDisclosureService.class);
    private final ProfileAccess access = new ProfileAccess(profiles, disclosures);
    private MockedStatic<StpUtil> token;
    private MockedStatic<LoginHelper> login;

    @BeforeEach
    void login() {
        token = mockStatic(StpUtil.class);
        login = mockStatic(LoginHelper.class);
        context(user());
    }

    @AfterEach
    void releaseStaticMocks() {
        login.close();
        token.close();
    }

    @Test
    void everyEntryChecksLoginBeforeReadingUserOrValidatingArguments() {
        NotLoginException anonymous = mock(NotLoginException.class);
        token.when(StpUtil::checkLogin).thenThrow(anonymous);
        assertThatThrownBy(access::currentSummary).isSameAs(anonymous);
        assertThatThrownBy(access::isPersonVerified).isSameAs(anonymous);
        assertThatThrownBy(access::isEnterpriseVerified).isSameAs(anonymous);
        assertThatThrownBy(() -> access.requireVerified((ProfileType[]) null)).isSameAs(anonymous);
        assertThatThrownBy(() -> access.currentDisclosure(null)).isSameAs(anonymous);
        login.verifyNoInteractions();
        verifyNoInteractions(profiles, disclosures);
    }

    @Test
    void summaryAndBooleanQueriesUseTheSessionOwnerAndNeverCacheAnEarlierApproval() {
        ProfileSummary verified = summary(7L, true, true);
        when(profiles.findByUserId(7L)).thenReturn(verified, verified, ProfileSummary.unverified(7L));
        assertThat(access.currentSummary()).isSameAs(verified);
        assertThat(access.isPersonVerified()).isTrue();
        assertThat(access.isPersonVerified()).isFalse();
        verify(profiles, times(3)).findByUserId(7L);
        verifyNoInteractions(disclosures);
    }

    @Test
    void enterpriseCertificationDoesNotImplicitlyRequirePersonCertification() {
        ProfileSummary enterprise = summary(7L, false, true);
        when(profiles.findByUserId(7L)).thenReturn(enterprise);
        assertThat(access.isEnterpriseVerified()).isTrue();
        assertThat(access.isPersonVerified()).isFalse();
        assertThat(access.requireVerified(ProfileType.ENTERPRISE)).isSameAs(enterprise);
        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> access.requireVerified(ProfileType.PERSON, ProfileType.ENTERPRISE))
            .satisfies(error -> assertError(error, "PERSON_VERIFICATION_REQUIRED",
                List.of("PERSON", "ENTERPRISE"), List.of("PERSON")));
    }

    @Test
    void gateUsesAndSemanticsWithStableDeduplicatedRequiredAndMissingTypes() {
        when(profiles.findByUserId(7L)).thenReturn(ProfileSummary.unverified(7L));
        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> access.requireVerified(ProfileType.ENTERPRISE, ProfileType.PERSON, ProfileType.ENTERPRISE))
            .satisfies(error -> assertError(error, "PROFILE_VERIFICATION_REQUIRED",
                List.of("PERSON", "ENTERPRISE"), List.of("PERSON", "ENTERPRISE")));
        when(profiles.findByUserId(7L)).thenReturn(summary(7L, true, false));
        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> access.requireVerified(ProfileType.ENTERPRISE))
            .satisfies(error -> assertError(error, "ENTERPRISE_VERIFICATION_REQUIRED",
                List.of("ENTERPRISE"), List.of("ENTERPRISE")));
        ProfileSummary both = summary(7L, true, true);
        when(profiles.findByUserId(7L)).thenReturn(both);
        assertThat(access.requireVerified(ProfileType.ENTERPRISE, ProfileType.PERSON)).isSameAs(both);
    }

    @Test
    void invalidRequirementsFailBeforeQuerying() {
        assertThatThrownBy(() -> access.requireVerified()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> access.requireVerified((ProfileType[]) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> access.requireVerified(ProfileType.PERSON, null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(profiles, disclosures);
    }

    @Test
    void missingUserOrMachineAndIncompleteClientContextsCannotQueryProfiles() {
        login.when(LoginHelper::getLoginUser).thenReturn(null);
        assertInvalidContext();
        List<Consumer<LoginUser>> invalid = List.of(
            value -> value.setUserId(null), value -> value.setUserId(0L), value -> value.setUserId(-1L),
            value -> value.setClientPk(null), value -> value.setClientPk(0L), value -> value.setClientPk(-1L),
            value -> value.setClientKey(null), value -> value.setClientKey(" "),
            value -> value.setUserType(null), value -> value.setUserType(" "),
            value -> value.setUserType("openapi"), value -> value.setUserType("OPENAPI"));
        for (Consumer<LoginUser> mutate : invalid) {
            LoginUser candidate = user();
            mutate.accept(candidate);
            login.when(LoginHelper::getLoginUser).thenReturn(candidate);
            assertInvalidContext();
        }
        verifyNoInteractions(profiles, disclosures);
    }

    @Test
    void tokenPrincipalAndClientExtrasMustMatchTheStoredLoginUser() {
        token.when(StpUtil::getLoginIdAsString).thenReturn("system:99");
        assertInvalidContext();
        context(user());
        for (Object invalid : Arrays.asList(null, 99L, 7.5, "not-a-user")) {
            token.when(() -> StpUtil.getExtra(LoginHelper.USER_KEY)).thenReturn(invalid);
            assertInvalidContext();
        }
        context(user());
        for (Object invalid : Arrays.asList(null, 999L, "wrong-client")) {
            token.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_PK_KEY)).thenReturn(invalid);
            assertInvalidContext();
        }
        context(user());
        token.when(() -> StpUtil.getExtra(LoginHelper.USER_TYPE_KEY)).thenReturn("another-domain");
        assertInvalidContext();
        context(user());
        for (Object invalid : Arrays.asList(null, " ", 100L)) {
            token.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_KEY)).thenReturn(invalid);
            assertInvalidContext();
        }
        verifyNoInteractions(profiles, disclosures);
    }

    @Test
    void numericTokenIdsAndOAuthClientStringRemainDistinctFromTheAppKey() {
        token.when(() -> StpUtil.getExtra(LoginHelper.USER_KEY)).thenReturn("7");
        token.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_PK_KEY)).thenReturn(101);
        when(profiles.findByUserId(7L)).thenReturn(ProfileSummary.unverified(7L));
        assertThat(access.currentSummary().userId()).isEqualTo(7L);
    }

    @Test
    void forgedHttpUserAndClientParametersCannotSelectAnotherProfileOwner() {
        var previous = RequestContextHolder.getRequestAttributes();
        var request = new MockHttpServletRequest();
        request.addParameter("userId", "999");
        request.addParameter("clientPk", "999");
        request.addHeader("clientid", "forged-client");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            when(profiles.findByUserId(7L)).thenReturn(ProfileSummary.unverified(7L));
            assertThat(access.currentSummary().userId()).isEqualTo(7L);
            verify(profiles).findByUserId(7L);
            verify(profiles, never()).findByUserId(999L);
        } finally {
            if (previous == null) RequestContextHolder.resetRequestAttributes();
            else RequestContextHolder.setRequestAttributes(previous);
        }
    }

    @Test
    void superAdminDoesNotBypassCertification() {
        LoginUser administrator = user();
        administrator.setUserId(1L);
        context(administrator);
        when(profiles.findByUserId(1L)).thenReturn(ProfileSummary.unverified(1L));
        assertThatExceptionOfType(ServiceException.class)
            .isThrownBy(() -> access.requireVerified(ProfileType.PERSON))
            .satisfies(error -> assertError(error, "PERSON_VERIFICATION_REQUIRED", List.of("PERSON"), List.of("PERSON")));
    }

    @Test
    void nullOrDifferentSubjectResultsFailClosedAndQueryErrorsPropagateUnchanged() {
        assertThatThrownBy(access::currentSummary).isInstanceOf(IllegalStateException.class);
        when(profiles.findByUserId(7L)).thenReturn(ProfileSummary.unverified(99L));
        assertThatThrownBy(access::isPersonVerified).isInstanceOf(IllegalStateException.class);
        RuntimeException queryFailure = new IllegalStateException("owned database unavailable");
        when(profiles.findByUserId(7L)).thenThrow(queryFailure);
        assertThatThrownBy(() -> access.requireVerified(ProfileType.PERSON)).isSameAs(queryFailure);
        Set<ProfileDisclosureField> fields = Set.of(ProfileDisclosureField.PERSON_FULL_NAME);
        assertThatThrownBy(() -> access.currentDisclosure(fields)).isInstanceOf(IllegalStateException.class);
        when(disclosures.findByUserId(7L, fields)).thenReturn(new ProfileDisclosure(99L, null, null));
        assertThatThrownBy(() -> access.currentDisclosure(fields)).isInstanceOf(IllegalStateException.class);
        when(disclosures.findByUserId(7L, fields)).thenThrow(queryFailure);
        assertThatThrownBy(() -> access.currentDisclosure(fields)).isSameAs(queryFailure);
    }

    @Test
    void disclosureSnapshotsTheWhitelistAndDoesNotLeakUnrequestedSensitiveFields() {
        var selected = new HashSet<>(Set.of(ProfileDisclosureField.PERSON_FULL_NAME,
            ProfileDisclosureField.PERSON_DOCUMENT_NUMBER_MASKED));
        var person = new PersonDisclosure(true, 71L, Instant.EPOCH, "已核准姓名", null, null,
            "ID_CARD", "**************1234", "sensitive-full-document", null, null);
        when(disclosures.findByUserId(eq(7L), anySet())).thenAnswer(call -> {
            Set<ProfileDisclosureField> actual = call.getArgument(1);
            assertThat(actual).containsExactlyInAnyOrderElementsOf(selected);
            assertThatThrownBy(() -> actual.add(ProfileDisclosureField.PERSON_DOCUMENT_NUMBER))
                .isInstanceOf(UnsupportedOperationException.class);
            return new ProfileDisclosure(7L, person, null);
        });
        ProfileDisclosure result = access.currentDisclosure(selected);
        assertThat(result.userId()).isEqualTo(7L);
        assertThat(result.person().fullName()).isEqualTo("已核准姓名");
        assertThat(result.person().documentNumberMasked()).isEqualTo("**************1234");
        assertThat(result.person().documentNumber()).isNull();
        assertThat(result.person().verified()).isNull();
        assertThat(result.person().profileId()).isNull();
        assertThat(result.enterprise()).isNull();
        verifyNoInteractions(profiles);
    }

    @Test
    void emptyAndInvalidDisclosureFieldSetsKeepTheirPublicContract() {
        assertThatThrownBy(() -> access.currentDisclosure(null)).isInstanceOf(IllegalArgumentException.class);
        Set<ProfileDisclosureField> invalid = new HashSet<>();
        invalid.add(null);
        assertThatThrownBy(() -> access.currentDisclosure(invalid)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(disclosures);
        when(disclosures.findByUserId(7L, Set.of())).thenReturn(new ProfileDisclosure(7L, null, null));
        assertThat(access.currentDisclosure(Set.of())).isEqualTo(new ProfileDisclosure(7L, null, null));
    }

    private void assertInvalidContext() {
        assertThatExceptionOfType(ServiceException.class).isThrownBy(access::currentSummary)
            .satisfies(error -> {
                assertThat(error.getCode()).isEqualTo(401);
                assertThat(error.getData()).isEqualTo(Map.of("reason", "PROFILE_LOGIN_CONTEXT_INVALID"));
            });
    }

    private void context(LoginUser user) {
        login.when(LoginHelper::getLoginUser).thenReturn(user);
        token.when(StpUtil::getLoginIdAsString).thenReturn(user.getLoginId());
        token.when(() -> StpUtil.getExtra(LoginHelper.USER_KEY)).thenReturn(user.getUserId());
        token.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_PK_KEY)).thenReturn(user.getClientPk());
        token.when(() -> StpUtil.getExtra(LoginHelper.USER_TYPE_KEY)).thenReturn(user.getUserType());
        token.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_KEY)).thenReturn("oauth-home-client-opaque");
    }

    private static LoginUser user() {
        var user = new LoginUser();
        user.setUserId(7L); user.setUserType("system"); user.setClientPk(101L); user.setClientKey("home-web");
        return user;
    }

    private static ProfileSummary summary(Long userId, boolean person, boolean enterprise) {
        return new ProfileSummary(userId,
            person ? new ProfileBindingSummary(71L, ProfileType.PERSON, Instant.EPOCH) : null,
            enterprise ? new ProfileBindingSummary(72L, ProfileType.ENTERPRISE, Instant.EPOCH) : null);
    }

    private static void assertError(ServiceException error, String reason, List<String> required, List<String> missing) {
        assertThat(error.getCode()).isEqualTo(403);
        assertThat(error.getData()).isEqualTo(Map.of("reason", reason, "requiredTypes", required, "missingTypes", missing));
    }
}
