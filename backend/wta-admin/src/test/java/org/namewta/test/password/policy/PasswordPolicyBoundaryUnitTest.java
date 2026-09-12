package org.namewta.test.password.policy;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.system.domain.bo.SysConfigBo;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.mapper.SysConfigMapper;
import org.namewta.system.password.PasswordCharacterClass;
import org.namewta.system.password.PasswordPolicyConfigParser;
import org.namewta.system.password.PasswordPolicyProjection;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.service.impl.SysConfigServiceImpl;
import org.namewta.web.controller.AuthController;
import org.namewta.web.domain.vo.AuthClientContextVo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Tag("dev")
class PasswordPolicyBoundaryUnitTest {

    @Test
    void shouldRejectInvalidPolicyBeforeDatabaseOrCacheMutation() {
        SysConfigMapper mapper = mock(SysConfigMapper.class);
        SysConfigServiceImpl service = new SysConfigServiceImpl(mapper,
            new PasswordPolicyConfigParser(JsonMapper.builder().build()));
        SysConfigBo bo = new SysConfigBo();
        bo.setConfigKey("sys.user.passwordPolicy");
        bo.setConfigValue("{\"version\":1,\"defaultPassword\":{\"mode\":\"FIXED\",\"fixedValue\":\"secret\"}}");

        ServiceException exception = assertThrows(ServiceException.class, () -> service.updateConfig(bo));

        assertEquals("PASSWORD_POLICY_UNAVAILABLE", exception.getMessage());
        assertFalse(exception.toString().contains("secret"));
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldExposeOnlyThePublicProjectionForEnabledClients() throws Exception {
        ISysClientService clientService = mock(ISysClientService.class);
        PasswordPolicyService policyService = mock(PasswordPolicyService.class);
        PasswordPolicyProjection projection = new PasswordPolicyProjection(8, 30,
            List.of(PasswordCharacterClass.UPPERCASE, PasswordCharacterClass.LOWERCASE,
                PasswordCharacterClass.DIGIT, PasswordCharacterClass.SPECIAL), "@$!%*?&");
        when(policyService.publicProjection()).thenReturn(projection);
        SysClientVo client = new SysClientVo();
        client.setStatus(SystemConstants.NORMAL);
        client.setRegisterEnabled(true);
        when(clientService.queryByClientId("web-client")).thenReturn(client);
        AuthController controller = new AuthController(null, null, null, null, clientService, null, policyService);

        AuthClientContextVo context = controller.clientContext("web-client", null).getData();

        assertTrue(context.getClientEnabled());
        assertTrue(context.getRegisterEnabled());
        assertEquals(projection, context.getPasswordPolicy());
        String json = JsonMapper.builder().build().writeValueAsString(context);
        assertTrue(json.contains("\"minimumLength\":8"));
        assertTrue(json.contains("\"requiredCharacterClasses\":[\"UPPERCASE\",\"LOWERCASE\",\"DIGIT\",\"SPECIAL\"]"));
        assertFalse(json.contains("mode"));
        assertFalse(json.contains("fixedValue"));
        assertFalse(json.contains("generator"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        String response = mockMvc.perform(get("/auth/client/context").param("clientId", "web-client"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.passwordPolicy.minimumLength").value(8))
            .andExpect(jsonPath("$.data.passwordPolicy.maximumLength").value(30))
            .andExpect(jsonPath("$.data.passwordPolicy.requiredCharacterClasses[3]").value("SPECIAL"))
            .andReturn().getResponse().getContentAsString();
        assertFalse(response.contains("fixedValue"));
        assertFalse(response.contains("generator"));

        AuthClientContextVo missing = controller.clientContext("missing", null).getData();
        assertFalse(missing.getClientEnabled());
        assertNull(missing.getPasswordPolicy());
    }
}
