package org.namewta.test.password.temporary;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import org.namewta.common.log.annotation.Log;
import org.namewta.system.controller.system.SysTemporaryPasswordController;
import org.namewta.system.domain.bo.password.TemporaryPasswordIssueBo;
import org.namewta.system.service.ISysUserService;
import org.namewta.system.temporarypassword.TemporaryPasswordAuditEnricher;
import org.namewta.system.temporarypassword.TemporaryPasswordService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class TemporaryPasswordControllerContractUnitTest {

    @Test
    void issueChecksUserScopeAndReturnsOneMinuteNoStoreResponse() throws Exception {
        ISysUserService userService = mock(ISysUserService.class);
        TemporaryPasswordService temporaryPasswordService = mock(TemporaryPasswordService.class);
        TemporaryPasswordAuditEnricher auditEnricher = mock(TemporaryPasswordAuditEnricher.class);
        when(temporaryPasswordService.issue(42L))
            .thenReturn(new TemporaryPasswordService.IssuedPassword("Temporary1!", 60));
        SysTemporaryPasswordController controller =
            new SysTemporaryPasswordController(userService, temporaryPasswordService, auditEnricher);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/system/user/temporaryPassword")
                .contentType("application/json").content("{\"userId\":42}"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("$.data.password").value("Temporary1!"))
            .andExpect(jsonPath("$.data.expiresInSeconds").value(60));

        var order = inOrder(userService, temporaryPasswordService, auditEnricher);
        order.verify(auditEnricher).attachTarget(42L);
        order.verify(userService).checkUserAllowed(42L);
        order.verify(userService).checkUserDataScope(42L);
        order.verify(temporaryPasswordService).issue(42L);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void issueHasIndependentPermissionAndNeverPersistsSensitiveBodies() throws Exception {
        var method = SysTemporaryPasswordController.class
            .getMethod("issue", TemporaryPasswordIssueBo.class, HttpServletResponse.class);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        Log log = method.getAnnotation(Log.class);

        assertArrayEquals(new String[]{"system:user:temporaryPassword"}, permission.value());
        assertFalse(log.isSaveRequestData());
        assertFalse(log.isSaveResponseData());
    }
}
