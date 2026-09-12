package org.namewta.common.openapi.registry;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import org.namewta.common.openapi.annotation.OpenApi;
import org.namewta.system.api.model.LoginUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class OpenApiOperationRegistryTest {

    @Test
    void publishesOnlyDirectlyAnnotatedRealMappingsWithSpringDocSchemas() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestWebConfiguration.class);
            context.refresh();
            RequestMappingHandlerMapping mappings = context.getBean(RequestMappingHandlerMapping.class);
            TestController controller = context.getBean(TestController.class);
            HandlerMethod open = handler(controller, "create", Long.class, boolean.class, OrderRequest.class);
            mappings.registerMapping(mapping("/unresolved"), controller, open.getMethod());

            OpenApiOperationRegistry registry = new OpenApiOperationRegistry(
                mappings, new SpringDocOperationSchemaResolver());
            registry.afterPropertiesSet();

            assertThat(registry.all()).hasSize(1);
            OpenApiOperationDefinition operation = registry.all().getFirst();
            assertThat(operation.interfaceId()).matches("[0-9a-f]{24}");
            assertThat(operation.summary()).isEqualTo("Create order");
            assertThat(operation.method()).isEqualTo("POST");
            assertThat(operation.path()).isEqualTo("/orders/{id}");
            assertThat(operation.parameters()).extracting(OpenApiParameterDefinition::name)
                .containsExactly("id", "verbose");
            assertThat(operation.parameters()).extracting(OpenApiParameterDefinition::location)
                .containsExactly("path", "query");
            assertThat(operation.requestSchema()).contains("OrderRequest", "components", "schemas");
            assertThat(operation.responseSchema()).contains("OrderResponse", "components", "schemas");
            assertThat(operation.accessRule().permissions()).hasSize(1);
            assertThat(operation.accessRule().roles()).hasSize(2);
            assertThat(registry.find(operation.interfaceId())).isSameAs(operation);
            assertThat(registry.rejectedMappings()).isEqualTo(2);
            assertThatThrownBy(() -> registry.all().add(operation))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void mirrorsSaTokenAndOrOrRoleAndWildcardSemantics() throws Exception {
        RequestMappingHandlerMapping mappings = mock(RequestMappingHandlerMapping.class);
        TestController controller = new TestController();
        HandlerMethod open = handler(controller, "create", Long.class, boolean.class, OrderRequest.class);
        when(mappings.getHandlerMethods()).thenReturn(Map.of(
            mapping("/orders/{id}", RequestMethod.POST), open));
        OpenApiOperationRegistry registry = new OpenApiOperationRegistry(
            mappings, new SpringDocOperationSchemaResolver());
        registry.afterPropertiesSet();
        OpenApiAccessRule rule = registry.all().getFirst().accessRule();
        OpenApiAuthorizationMatcher matcher = new OpenApiAuthorizationMatcher();

        assertThat(matcher.matches(user(Set.of("system:orders:create"), Set.of("tenant", "active")), rule)).isTrue();
        assertThat(matcher.matches(user(Set.of("system:orders:submit"), Set.of("tenant", "active")), rule)).isTrue();
        assertThat(matcher.matches(user(Set.of(), Set.of("tenant", "active", "operator")), rule)).isTrue();
        assertThat(matcher.matches(user(Set.of("*:*:*"), Set.of("tenant", "active")), rule)).isTrue();
        assertThat(matcher.matches(user(Set.of("system:orders:create"), Set.of("tenant")), rule)).isFalse();
        assertThat(matcher.matches(user(Set.of(), Set.of("tenant", "active")), rule)).isFalse();
        assertThat(matcher.matches(null, rule)).isFalse();
    }

    private static RequestMappingInfo mapping(String path, RequestMethod... methods) {
        RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
        options.setPatternParser(new PathPatternParser());
        return RequestMappingInfo.paths(path).methods(methods).options(options).build();
    }

    private static HandlerMethod handler(Object controller, String name, Class<?>... parameterTypes)
        throws NoSuchMethodException {
        Method method = controller.getClass().getDeclaredMethod(name, parameterTypes);
        return new HandlerMethod(controller, method);
    }

    private static LoginUser user(Set<String> permissions, Set<String> roles) {
        LoginUser user = new LoginUser();
        user.setMenuPermission(permissions);
        user.setRolePermission(roles);
        return user;
    }

    @RestController
    @RequestMapping("/orders")
    @SaCheckRole("tenant")
    static final class TestController {

        @OpenApi("Create order")
        @SaCheckPermission(value = {"system:orders:create", "system:orders:submit"}, mode = SaMode.OR,
            orRole = {"operator", "auditor"})
        @SaCheckRole("active")
        @PostMapping("/{id}")
        OrderResponse create(@PathVariable("id") Long id,
                             @RequestParam(value = "verbose", required = false) boolean verbose,
                             @RequestBody OrderRequest request) {
            return new OrderResponse(id, request.name());
        }

        @GetMapping("/internal")
        OrderResponse hidden() {
            return new OrderResponse(1L, "hidden");
        }

        @OpenApi("Unsupported bypass")
        @SaIgnore
        @GetMapping("/ignored")
        OrderResponse ignored() {
            return new OrderResponse(2L, "ignored");
        }
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfiguration {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    record OrderRequest(String name, int quantity) {
    }

    record OrderResponse(Long id, String name) {
    }
}
