package org.namewta.test.profile.contract;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.namewta.common.core.domain.R;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.common.satoken.profile.ProfileAccess;
import org.namewta.common.satoken.profile.ProfileVerificationAdvisor;
import org.namewta.common.satoken.profile.annotation.RequireEnterpriseVerified;
import org.namewta.common.satoken.profile.annotation.RequirePersonVerified;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.handler.GlobalExceptionHandler;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.domain.ProfileBindingSummary;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.system.api.model.LoginUser;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 实际 MVC、SaInterceptor、档案访问门禁和现有异常处理器的传输合同。 */
@Tag("dev")
class ProfileVerificationHttpBoundaryTest {

    @Test
    void anonymousSaIgnoreEndpointStillRequiresLoginAndDoesNotReadProfiles() throws Exception {
        try (Fixture fixture = new Fixture(7L)) {
            fixture.stp.when(StpUtil::checkLogin).thenThrow(NotLoginException.newInstance(
                "login", NotLoginException.NOT_TOKEN, "not logged in", null));

            fixture.mvc.perform(get("/profile-gate/person"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

            assertThat(fixture.controller.calls.get()).isZero();
            assertThat(fixture.saAuthCalls.get()).isZero();
            verifyNoInteractions(fixture.profiles);
        }
    }

    @Test
    void unverifiedPersonReturnsExistingBusiness403WithStableReason() throws Exception {
        try (Fixture fixture = new Fixture(7L)) {
            when(fixture.profiles.findByUserId(7L)).thenReturn(ProfileSummary.unverified(7L));

            fixture.mvc.perform(get("/profile-gate/person"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.data.reason").value("PERSON_VERIFICATION_REQUIRED"))
                .andExpect(jsonPath("$.data.missingTypes[0]").value("PERSON"));

            assertThat(fixture.controller.calls.get()).isZero();
            assertThat(fixture.saAuthCalls.get()).isZero();
        }
    }

    @Test
    void combinedAnnotationsUseOneQueryAndReportOnlyMissingEnterprise() throws Exception {
        try (Fixture fixture = new Fixture(7L)) {
            when(fixture.profiles.findByUserId(7L)).thenReturn(new ProfileSummary(7L,
                binding(ProfileType.PERSON), null));

            fixture.mvc.perform(get("/profile-gate/both"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.data.reason").value("ENTERPRISE_VERIFICATION_REQUIRED"))
                .andExpect(jsonPath("$.data.requiredTypes.length()").value(2))
                .andExpect(jsonPath("$.data.missingTypes.length()").value(1));

            assertThat(fixture.controller.calls.get()).isZero();
            verify(fixture.profiles).findByUserId(7L);
            verifyNoMoreInteractions(fixture.profiles);
        }
    }

    @Test
    void successfulCombinedGateExecutesControllerExactlyOnce() throws Exception {
        try (Fixture fixture = new Fixture(7L)) {
            when(fixture.profiles.findByUserId(7L)).thenReturn(new ProfileSummary(7L,
                binding(ProfileType.PERSON), binding(ProfileType.ENTERPRISE)));

            fixture.mvc.perform(get("/profile-gate/both"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("both"));

            assertThat(fixture.controller.calls.get()).isEqualTo(1);
            verify(fixture.profiles).findByUserId(7L);
            verifyNoMoreInteractions(fixture.profiles);
        }
    }

    @Test
    void superAdminIsNotExemptFromVerification() throws Exception {
        try (Fixture fixture = new Fixture(1L)) {
            fixture.login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            when(fixture.profiles.findByUserId(1L)).thenReturn(ProfileSummary.unverified(1L));

            fixture.mvc.perform(get("/profile-gate/person"))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.data.reason").value("PERSON_VERIFICATION_REQUIRED"));

            assertThat(fixture.controller.calls.get()).isZero();
            verify(fixture.profiles).findByUserId(1L);
        }
    }

    @Test
    void profileQueryFailureIsNotReportedAsAnUnverifiedAccount() throws Exception {
        try (Fixture fixture = new Fixture(7L)) {
            when(fixture.profiles.findByUserId(7L)).thenThrow(new IllegalStateException("store unavailable"));

            fixture.mvc.perform(get("/profile-gate/person"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.data.reason").doesNotExist());

            assertThat(fixture.controller.calls.get()).isZero();
        }
    }

    private static ProfileBindingSummary binding(ProfileType type) {
        return new ProfileBindingSummary(type == ProfileType.PERSON ? 91L : 92L,
            type, Instant.parse("2026-01-01T00:00:00Z"));
    }

    /** 所有线程级静态替身在每次用例结束时关闭，不改写 SaManager 的全局实现。 */
    private static final class Fixture implements AutoCloseable {
        final ProfileService profiles = mock(ProfileService.class);
        final GuardedController controller = new GuardedController();
        final AtomicInteger saAuthCalls = new AtomicInteger();
        final MockedStatic<StpUtil> stp = mockStatic(StpUtil.class);
        final MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class);
        final MockMvc mvc;

        Fixture(long userId) {
            try {
                mvc = createMvc(userId);
            } catch (RuntimeException | Error failure) {
                close();
                throw failure;
            }
        }

        private MockMvc createMvc(long userId) {
            LoginUser user = new LoginUser();
            user.setUserId(userId);
            user.setClientPk(31L);
            user.setClientKey("home");
            user.setUserType("app_user");
            login.when(LoginHelper::getLoginUser).thenReturn(user);
            stp.when(StpUtil::getLoginIdAsString).thenReturn(user.getLoginId());
            stp.when(() -> StpUtil.getExtra(LoginHelper.USER_KEY)).thenReturn(userId);
            stp.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_PK_KEY)).thenReturn(31L);
            stp.when(() -> StpUtil.getExtra(LoginHelper.USER_TYPE_KEY)).thenReturn("app_user");
            stp.when(() -> StpUtil.getExtra(LoginHelper.CLIENT_KEY)).thenReturn("home-oauth-client");
            DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
            beans.registerSingleton("profileAccess", new ProfileAccess(profiles, mock(ProfileDisclosureService.class)));
            ProxyFactory factory = new ProxyFactory(controller);
            factory.setProxyTargetClass(true);
            factory.addAdvisor(new ProfileVerificationAdvisor(beans.getBeanProvider(ProfileAccess.class)));
            return MockMvcBuilders.standaloneSetup(factory.getProxy())
                .setControllerAdvice(new SaTokenExceptionHandler(), new GlobalExceptionHandler())
                .addInterceptors(new SaInterceptor(handler -> saAuthCalls.incrementAndGet()))
                .build();
        }

        @Override
        public void close() {
            login.close();
            stp.close();
        }
    }

    @RestController
    @SaIgnore
    @RequirePersonVerified
    public static class GuardedController {
        final AtomicInteger calls = new AtomicInteger();

        @GetMapping("/profile-gate/person")
        public R<String> person() {
            calls.incrementAndGet();
            return R.data("person");
        }

        @GetMapping("/profile-gate/both")
        @RequireEnterpriseVerified
        public R<String> both() {
            calls.incrementAndGet();
            return R.data("both");
        }
    }
}
