package org.namewta.test.notify;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.satoken.handler.SaTokenExceptionHandler;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationCancelCommand;
import org.namewta.notify.api.NotificationRetryCommand;
import org.namewta.notify.controller.admin.NotificationController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** HTTP 主键必须来自路径；冲突请求不能抵达业务服务。 */
@Tag("dev")
class NotificationRetryControllerContractTest {

    @Test
    void retryRejectsConflictingBodyIntentBeforeCallingService() {
        NotificationApplicationService service = mock(NotificationApplicationService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service)).build();

        Throwable failure = catchThrowable(() -> mvc.perform(post("/notify/notification/101/retry")
            .contentType("application/json")
            .content("{\"notificationId\":\"202\",\"deliveryId\":\"303\",\"reason\":\"manual\"}")));

        verifyNoInteractions(service);
        assertThat(failure).isNotNull().hasRootCauseInstanceOf(ServiceException.class);
    }

    @Test
    void cancelRejectsConflictingBodyIntentBeforeCallingService() {
        NotificationApplicationService service = mock(NotificationApplicationService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service)).build();

        Throwable failure = catchThrowable(() -> mvc.perform(post("/notify/notification/101/cancel")
            .contentType("application/json").content("{\"notificationId\":\"202\",\"reason\":\"manual\"}")));

        verifyNoInteractions(service);
        assertThat(failure).isNotNull().hasRootCauseInstanceOf(ServiceException.class);
    }

    @Test
    void emptyBodyUsesPathAsCanonicalIntentForBothOperations() throws Exception {
        NotificationApplicationService service = mock(NotificationApplicationService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service)).build();

        mvc.perform(post("/notify/notification/101/retry")).andReturn();
        mvc.perform(post("/notify/notification/101/cancel")).andReturn();

        verify(service).retry(new NotificationRetryCommand("101", null, "manual", null));
        verify(service).cancel(new NotificationCancelCommand("101", "manual"));
    }

    @Test
    void actualSaInterceptorSeparatelyEnforcesRetryAndCancelPermissions() throws Exception {
        var previousConfig = SaManager.getConfig();
        var previousContext = SaManager.getSaTokenContext();
        var previousDao = SaManager.getSaTokenDao();
        var previousPermissions = SaManager.getStpInterface();
        var previousLogic = StpUtil.getStpLogic();
        try {
            SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer"));
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
            var grants = new AtomicReference<List<String>>(List.of());
            SaManager.setStpInterface(new StpInterface() {
                @Override public List<String> getPermissionList(Object loginId, String loginType) { return grants.get(); }
                @Override public List<String> getRoleList(Object loginId, String loginType) { return List.of(); }
            });
            StpUtil.setStpLogic(new StpLogic("login"));
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest(), new MockHttpServletResponse()));
            StpUtil.login(938001L);
            String token = StpUtil.getTokenValue();
            NotificationApplicationService service = mock(NotificationApplicationService.class);
            var mvc = MockMvcBuilders.standaloneSetup(new NotificationController(service))
                .addInterceptors(new SaInterceptor()).setControllerAdvice(new SaTokenExceptionHandler()).build();

            mvc.perform(post("/notify/notification/101/retry").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(403));
            mvc.perform(post("/notify/notification/101/cancel").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(403));
            verifyNoInteractions(service);

            grants.set(List.of("notify:notification:retry"));
            mvc.perform(post("/notify/notification/101/retry").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(200));
            mvc.perform(post("/notify/notification/101/cancel").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(403));
            verify(service).retry(new NotificationRetryCommand("101", null, "manual", null));

            grants.set(List.of("notify:notification:cancel"));
            mvc.perform(post("/notify/notification/101/retry").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(403));
            mvc.perform(post("/notify/notification/101/cancel").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(200));
            verify(service).cancel(new NotificationCancelCommand("101", "manual"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
            SaManager.setConfig(previousConfig);
            SaManager.setSaTokenContext(previousContext);
            SaManager.setSaTokenDao(previousDao);
            SaManager.setStpInterface(previousPermissions);
            StpUtil.setStpLogic(previousLogic);
        }
    }
}
