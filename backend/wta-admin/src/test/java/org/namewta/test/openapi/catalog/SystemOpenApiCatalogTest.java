package org.namewta.test.openapi.catalog;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.openapi.catalog.OpenApiCatalogItem;
import org.namewta.common.openapi.registry.OpenApiAccessRule;
import org.namewta.common.openapi.registry.OpenApiAuthorizationMatcher;
import org.namewta.common.openapi.registry.OpenApiOperationDefinition;
import org.namewta.common.openapi.registry.OpenApiOperationRegistry;
import org.namewta.common.openapi.spi.OpenApiAuthorizationResolver;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.namewta.system.controller.system.openapi.SysOpenApiCatalogController;
import org.namewta.system.openapi.catalog.SystemOpenApiCatalogService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class SystemOpenApiCatalogTest {

    @Test
    void filtersOnlyByTargetAuthorizationWithoutCredentialOrSessionState() {
        OpenApiOperationRegistry registry = mock(OpenApiOperationRegistry.class);
        OpenApiAuthorizationResolver resolver = mock(OpenApiAuthorizationResolver.class);
        OpenApiOperationDefinition allowed = operation("allowed", "orders:read");
        OpenApiOperationDefinition denied = operation("denied", "orders:write");
        when(registry.all()).thenReturn(List.of(allowed, denied));
        when(registry.find("allowed")).thenReturn(allowed);
        when(registry.find("denied")).thenReturn(denied);
        when(resolver.resolve(41L)).thenReturn(user("orders:read"));
        SystemOpenApiCatalogService service = new SystemOpenApiCatalogService(
            registry, resolver, new OpenApiAuthorizationMatcher());

        assertThat(service.list(41L)).extracting(OpenApiCatalogItem::interfaceId).containsExactly("allowed");
        OpenApiCatalogItem detail = service.detail(41L, "allowed");
        assertThat(detail.curlExample()).contains("<APP_KEY>", "<NAMEWTA_V1_SIGNATURE>")
            .contains("Content-Type: application/json", "<JSON_BODY>")
            .doesNotContain("secret");
        assertThat(detail.javaExample()).contains("X-OpenAPI-Version", "X-Nonce", "BodyPublishers.ofString");
        assertThatThrownBy(() -> service.detail(41L, "denied"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("OpenAPI interface is unavailable");
        verify(resolver, org.mockito.Mockito.times(3)).resolve(41L);
    }

    @Test
    void exposesSelfAndSuperAdminHttpContractsAndRechecksAdminServerSide() throws Exception {
        SystemOpenApiCatalogService service = mock(SystemOpenApiCatalogService.class);
        OpenApiCatalogItem item = item("allowed");
        when(service.list(7L)).thenReturn(List.of(item));
        when(service.detail(7L, "allowed")).thenReturn(item);
        when(service.list(9L)).thenReturn(List.of(item));
        when(service.detail(9L, "allowed")).thenReturn(item);
        SysOpenApiCatalogController controller = new SysOpenApiCatalogController(service);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(7L);
            login.when(LoginHelper::isSuperAdmin).thenReturn(true);
            mvc.perform(get("/system/openApi/self/interfaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].interfaceId").value("allowed"));
            mvc.perform(get("/system/openApi/self/interfaces/allowed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceId").value("allowed"));
            mvc.perform(get("/system/openApi/users/9/interfaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].interfaceId").value("allowed"));
            mvc.perform(get("/system/openApi/users/9/interfaces/allowed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceId").value("allowed"));
        }

        try (MockedStatic<LoginHelper> login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::isSuperAdmin).thenReturn(false);
            assertThatThrownBy(() -> controller.userInterfaces(9L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("OpenAPI target catalog is unavailable");
        }
    }

    private static OpenApiOperationDefinition operation(String id, String permission) {
        OpenApiAccessRule rule = new OpenApiAccessRule(
            List.of(new OpenApiAccessRule.PermissionRule(
                List.of(permission), OpenApiAccessRule.Mode.AND, List.of())), List.of());
        return new OpenApiOperationDefinition(id, "Order query", "POST", "/orders", rule,
            List.of(), "{\"type\":\"object\"}", "{\"type\":\"object\"}");
    }

    private static LoginUser user(String permission) {
        LoginUser user = new LoginUser();
        user.setMenuPermission(Set.of(permission));
        user.setRolePermission(Set.of());
        return user;
    }

    private static OpenApiCatalogItem item(String id) {
        return new OpenApiCatalogItem(id, "Order query", "GET", "/orders",
            new OpenApiAccessRule(List.of(), List.of()), List.of(), null, "{}", "curl", "java");
    }
}
