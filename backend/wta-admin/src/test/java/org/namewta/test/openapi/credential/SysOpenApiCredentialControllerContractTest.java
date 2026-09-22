package org.namewta.test.openapi.credential;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import org.namewta.common.core.constant.HttpStatus;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.controller.system.openapi.SysOpenApiCredentialController;
import org.namewta.system.openapi.credential.model.OpenApiCredentialCreateRequest;
import org.namewta.system.openapi.credential.model.OpenApiCredentialIssued;
import org.namewta.system.openapi.credential.model.OpenApiCredentialSummary;
import org.namewta.system.openapi.credential.service.SystemOpenApiCredentialService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class SysOpenApiCredentialControllerContractTest {

    @Test
    void missingCredentialReturnsEmptySuccess() {
        SystemOpenApiCredentialService service = mock(SystemOpenApiCredentialService.class);
        SysOpenApiCredentialController controller = new SysOpenApiCredentialController(service);
        when(service.get(41L)).thenReturn(null);

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(41L);
            R<OpenApiCredentialSummary> self = controller.getSelfCredential();
            assertThat(self.getCode()).isEqualTo(HttpStatus.SUCCESS);
            assertThat(self.getData()).isNull();
        }

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            R<OpenApiCredentialSummary> target = controller.getUserCredential(41L);
            assertThat(target.getCode()).isEqualTo(HttpStatus.SUCCESS);
            assertThat(target.getData()).isNull();
        }
    }

    @Test
    void selfOwnerComesOnlyFromLoginContextAndAdminRechecksSuperAdmin() {
        SystemOpenApiCredentialService service = mock(SystemOpenApiCredentialService.class);
        SysOpenApiCredentialController controller = new SysOpenApiCredentialController(service);
        OpenApiCredentialCreateRequest request = new OpenApiCredentialCreateRequest("billing", null, null);
        HttpServletResponse response = mock(HttpServletResponse.class);

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(41L);
            controller.createSelf(request, response);
            verify(service).create(41L, request);
            verify(response).setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        }

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(false);
            assertThatThrownBy(() -> controller.getUserCredential(42L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("OpenAPI credential management is unavailable");
        }
    }

    @Test
    void everyWriteUsesPostExactPermissionAndSafeOperationLogging() throws Exception {
        assertWrite("createSelf", BusinessType.INSERT, "system:openApi:self");
        assertWrite("resetSelf", BusinessType.UPDATE, "system:openApi:self");
        assertWrite("enableSelf", BusinessType.UPDATE, "system:openApi:self");
        assertWrite("disableSelf", BusinessType.UPDATE, "system:openApi:self");
        assertWrite("deleteSelf", BusinessType.DELETE, "system:openApi:self");
        assertWrite("createUser", BusinessType.INSERT, "system:openApi:add", Long.class,
            OpenApiCredentialCreateRequest.class, HttpServletResponse.class);
        assertWrite("resetUser", BusinessType.UPDATE, "system:openApi:edit", Long.class,
            HttpServletResponse.class);
        assertWrite("enableUser", BusinessType.UPDATE, "system:openApi:edit", Long.class);
        assertWrite("disableUser", BusinessType.UPDATE, "system:openApi:edit", Long.class);
        assertWrite("deleteUser", BusinessType.DELETE, "system:openApi:remove", Long.class);
    }

    @Test
    void transportShowsSecretOnlyForCreateAndUsesNoStore() throws Exception {
        SystemOpenApiCredentialService service = mock(SystemOpenApiCredentialService.class);
        SysOpenApiCredentialController controller = new SysOpenApiCredentialController(service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        OpenApiCredentialSummary summary = new OpenApiCredentialSummary(91L, 41L, "app-key", "billing",
            "0", null, null, LocalDateTime.now(), LocalDateTime.now());
        OpenApiCredentialIssued issued = new OpenApiCredentialIssued(91L, 41L, "app-key", "one-time-secret",
            "billing", "0", null, null, summary.createTime(), summary.updateTime());
        when(service.get(41L)).thenReturn(summary);
        when(service.create(org.mockito.ArgumentMatchers.eq(41L),
            org.mockito.ArgumentMatchers.any(OpenApiCredentialCreateRequest.class))).thenReturn(issued);

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(41L);
            mvc.perform(get("/system/openApi/self/credential"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appKey").value("app-key"))
                .andExpect(jsonPath("$.data.appSecret").doesNotExist())
                .andExpect(jsonPath("$.data.secretCiphertext").doesNotExist());
            mvc.perform(post("/system/openApi/self/credential/create")
                    .contentType("application/json")
                    .content("{\"appName\":\"billing\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.data.appSecret").value("one-time-secret"));
        }
    }

    private static void assertWrite(String name, BusinessType type, String permission,
                                    Class<?>... explicitTypes) throws Exception {
        Class<?>[] types = explicitTypes;
        if (types.length == 0 && name.equals("createSelf")) {
            types = new Class<?>[]{OpenApiCredentialCreateRequest.class, HttpServletResponse.class};
        } else if (types.length == 0 && name.equals("resetSelf")) {
            types = new Class<?>[]{HttpServletResponse.class};
        }
        Method method = SysOpenApiCredentialController.class.getMethod(name, types);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).singleElement().asString().endsWith("/credential/" + action(name));
        SaCheckPermission check = method.getAnnotation(SaCheckPermission.class);
        assertThat(check.value()).containsExactly(permission);
        Log log = method.getAnnotation(Log.class);
        assertThat(log.businessType()).isEqualTo(type);
        assertThat(log.isSaveRequestData()).isFalse();
        assertThat(log.isSaveResponseData()).isFalse();
    }

    private static String action(String methodName) {
        for (String action : new String[]{"create", "reset", "enable", "disable", "delete"}) {
            if (methodName.toLowerCase().startsWith(action)) {
                return action;
            }
        }
        throw new IllegalArgumentException(methodName);
    }
}
