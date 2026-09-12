package org.namewta.test.password.write;

import jakarta.servlet.http.HttpServletResponse;
import org.namewta.common.log.annotation.Log;
import org.namewta.system.controller.system.SysUserCredentialController;
import org.namewta.system.controller.system.SysProfileController;
import org.namewta.system.controller.system.SysUserController;
import org.namewta.system.domain.bo.password.ResetPasswordCandidateBo;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.vo.password.ResetPasswordCandidateVo;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysUserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class PasswordCandidateContractUnitTest {

    @Test
    void candidateChecksResetScopeReturnsNoStoreAndDoesNotMutateUser() throws Exception {
        ISysUserService userService = mock(ISysUserService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        when(policyService.generateDefaultPassword()).thenReturn("Candidate1!");
        SysUserCredentialController controller = new SysUserCredentialController(userService, policyService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/system/user/resetPwd/candidate")
                .contentType("application/json").content("{\"userId\":42}"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("$.data.password").value("Candidate1!"));

        var order = inOrder(userService, policyService);
        order.verify(userService).checkUserAllowed(42L);
        order.verify(userService).checkUserDataScope(42L);
        order.verify(policyService).generateDefaultPassword();
        verifyNoMoreInteractions(userService);
    }

    @Test
    void addInitializationReturnsPolicyCandidateWithoutCaching() throws Exception {
        ISysUserService userService = mock(ISysUserService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        when(policyService.generateDefaultPassword()).thenReturn("NewUserPass1!");
        SysUserController controller = new SysUserController(userService,
            mock(org.namewta.system.service.ISysRoleService.class),
            mock(org.namewta.system.service.ISysPostService.class),
            mock(org.namewta.system.service.ISysDeptService.class), policyService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/system/user/"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("$.data.password").value("NewUserPass1!"));
    }

    @Test
    void candidateAuditNeverPersistsRequestOrResponseBodies() throws Exception {
        Log log = SysUserCredentialController.class
            .getMethod("candidate", ResetPasswordCandidateBo.class, HttpServletResponse.class)
            .getAnnotation(Log.class);

        assertFalse(log.isSaveRequestData());
        assertFalse(log.isSaveResponseData());
        String json = tools.jackson.databind.json.JsonMapper.builder().build()
            .writeValueAsString(new ResetPasswordCandidateVo("Candidate1!"));
        assertTrue(json.equals("{\"password\":\"Candidate1!\"}"));

        Log resetLog = SysUserController.class.getMethod("resetPwd", SysUserBo.class).getAnnotation(Log.class);
        assertArrayEquals(new String[]{"password"}, resetLog.excludeParamNames());
        Log profileLog = SysProfileController.class
            .getMethod("updatePwd", SysProfileController.SysUserPasswordBo.class).getAnnotation(Log.class);
        assertFalse(profileLog.isSaveRequestData());
        assertFalse(profileLog.isSaveResponseData());
    }
}
